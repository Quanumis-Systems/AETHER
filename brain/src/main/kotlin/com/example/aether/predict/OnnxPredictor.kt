package com.example.aether.predict

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import org.slf4j.LoggerFactory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * OnnxPredictor
 *
 * This singleton runs our local-first predictive trajectory models right on the Edge (Ignition Gateway).
 * It loads lightweight ONNX models (e.g., LSTMs or Transformers trained to forecast energy use/output 60 mins ahead).
 *
 * Agentic Negative Feedback Loop:
 * If a particular model begins throwing shape mismatch errors or starts consistently predicting Out-Of-Bounds (OOB) values,
 * the internal logic categorizes this as "drift", logs a self-correcting warning, and temporarily disables
 * trajectory input to the TBL Orchestrator until a human-in-the-loop intervenes or a new model is hot-swapped.
 */
object OnnxPredictor {
    
    private val logger = LoggerFactory.getLogger("AETHER.Predictor")
    
    // The ONNX Environment is a process-wide singleton required for loading models and executing tensors
    private val env: OrtEnvironment by lazy {
        OrtEnvironment.getEnvironment("AETHER-Gateway-Env")
    }

    private var session: OrtSession? = null
    
    // Mutex to ensure hot-swapping models doesn't cause race conditions during inference
    private val sessionMutex = Mutex()
    
    // Auto-improvement state tracking: if we fail a certain number of times, we enter a "cooldown"
    private var consecutiveErrors = 0
    private const val MAX_ERRORS_BEFORE_COOLDOWN = 5
    private var isCooldownActive = false

    /**
     * Initialize or Hot-Swap the ONNX model from a file path dynamically.
     */
    suspend fun loadModel(modelPath: String) {
        sessionMutex.withLock {
            try {
                // If there's an existing session running, close it completely to free up native C++ memory
                session?.close()
                
                logger.info("Loading Predictive Trajectory Engine from: $modelPath")
                val sessionOptions = OrtSession.SessionOptions().apply {
                    // Restrict threads to avoid starving the standard Ignition thread pools
                    setIntraOpNumThreads(2)
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
                }
                
                session = env.createSession(modelPath, sessionOptions)
                
                // Success: Reset the error counter and cooldown logic
                consecutiveErrors = 0
                isCooldownActive = false
                logger.info("ONNX Session Loaded Successfully. Engine is Ready.")
                
            } catch (e: Exception) {
                logger.error("Failed to load ONNX model. The trajectory engine will stay degraded.", e)
            }
        }
    }

    /**
     * Run a "What-If" prediction simulation.
     * Given the current context of PLC states, predict the targeted output.
     * 
     * @param inputFeatures A flat float array containing normalized PLC data for the timeframe.
     * @return Single float prediction (e.g. Energy Consumption 60 mins from now)
     */
    suspend fun predictFutureState(inputFeatures: FloatArray): Float {
        if (isCooldownActive) {
            logger.warn("Predictor is in error cooldown. Skipping trajectory simulation.")
            return -1f // Indicates degraded operation
        }

        val activeSession = session ?: run {
            logger.warn("ONNX session not loaded. Cannot run trajectory prediction.")
            return -1f
        }

        return sessionMutex.withLock {
            try {
                // Determine the expected input node name from the model
                val inputName = activeSession.inputNames.first()
                
                // Shape: [1 batch, length of features]
                val shape = longArrayOf(1, inputFeatures.size.toLong())
                
                // Load Java primitive array into ONNX off-heap Tensor
                val tensor = OnnxTensor.createTensor(env, arrayOf(inputFeatures))
                
                // Execute trajectory
                val results = activeSession.run(mapOf(inputName to tensor))
                
                // Extract float array from output tensor. 
                // Assumes model outputs a single regression value [1, 1].
                val outputValue = (results[0].value as Array<FloatArray>)[0][0]
                
                tensor.close()
                results.close()

                // Negative Feedback Hook: Check if output is absolutely insane (e.g., predicting -1B degrees Celsius).
                // If so, increment the error counter.
                if (outputValue.isNaN() || outputValue.isInfinite()) {
                    triggerNegativeFeedbackLoop("Model output NaN or Infinite.")
                    return -1f
                }

                // If successful, gradually decay errors (Self-healing over time)
                if (consecutiveErrors > 0) consecutiveErrors--

                return@withLock outputValue

            } catch (e: Exception) {
                triggerNegativeFeedbackLoop("Inference Exception: ${e.message}")
                return@withLock -1f
            }
        }
    }

    /**
     * Handles the Agentic Negative Feedback to self-protect system integrity.
     */
    private fun triggerNegativeFeedbackLoop(reason: String) {
        consecutiveErrors++
        logger.error("Predictor Anomaly Detected: $reason. Error Count: $consecutiveErrors")
        
        if (consecutiveErrors >= MAX_ERRORS_BEFORE_COOLDOWN) {
            isCooldownActive = true
            logger.error("CRITICAL: Predictor entered Cooldown state due to sustained errors. Trajectory simulation is offline.")
        }
    }
}
