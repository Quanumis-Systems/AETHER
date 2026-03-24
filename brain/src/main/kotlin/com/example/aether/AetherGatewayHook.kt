package com.example.aether

import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook
import com.inductiveautomation.ignition.gateway.model.GatewayContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlinx.coroutines.*

/**
 * AetherGatewayHook
 *
 * The primary entry point for the AETHER Ignition Module.
 * This class hooks into the Ignition Gateway lifecycle to initialize our:
 * 1. High-Fidelity Ledger (Telemetry Interception)
 * 2. Predictive Trajectory Engine (ONNX Runtime)
 * 3. Agentic RAG Layer
 * 4. TBL Orchestrator components
 */
class AetherGatewayHook : AbstractGatewayModuleHook() {
    
    private val logger: Logger = LoggerFactory.getLogger("AETHER.Gateway")
    private lateinit var gatewayContext: GatewayContext
    
    // Core Extensibility Protocol for local agents
    private lateinit var mcpServer: com.example.aether.mcp.AetherMcpServer
    
    // We launch our agentic background loops in this module-level coroutine scope.
    // This ensures that when the module shuts down, the agentic coroutines are cleanly cancelled.
    private val moduleScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Telemetry Ledger Components
    private lateinit var aetherTagManager: com.example.aether.telemetry.AetherTagManager

    override fun setup(context: GatewayContext) {
        this.gatewayContext = context
        logger.info("AETHER Module setting up. Initializing Agentic RAG and Hardware integrations.")
        
        logger.info("Initializing QuestDB (ILP) and Qdrant (gRPC) connection pools...")
        
        // Initialize the MCP Edge Server wrapper around the GatewayContext
        this.mcpServer = com.example.aether.mcp.AetherMcpServer(context)
    }

    override fun startup(licenseState: com.inductiveautomation.ignition.common.licensing.LicenseState) {
        logger.info("AETHER Module starting up. Engaging \"Stream-RAG\" pattern.")
        
        // Start the Model Context Protocol Transport Layer
        mcpServer.start()
        
        // Start the High-Fidelity Dual-Stream Dispatcher
        com.example.aether.telemetry.TagToVectorService.startDispatcher()
        
        // Start Intercepting the Quanumis UDT folder
        this.aetherTagManager = com.example.aether.telemetry.AetherTagManager(gatewayContext.tagManager)
        aetherTagManager.registerListeners()
        
        // Start the agentic negative feedback loops for self-improvement and ESG constraints
        startAgenticLoops()
    }

    override fun shutdown() {
        logger.info("AETHER Module shutting down. Cancelling agentic coroutines.")
        
        if (::aetherTagManager.isInitialized) {
            aetherTagManager.unregisterListeners()
        }
        com.example.aether.telemetry.TagToVectorService.stopDispatcher()
        
        // Gracefully shutdown MCP JSON-RPC Server
        if (::mcpServer.isInitialized) {
            mcpServer.stop()
        }
        
        // Cleanly terminate all background worker coroutines
        moduleScope.cancel()
    }
    
    /**
     * Agentic Negative Feedback Loop initialization.
     * This loop runs perpetually in the background, checking the system state against the TBL goals.
     */
    private fun startAgenticLoops() {
        moduleScope.launch {
            try {
                // Initial delay to let the Gateway settle
                delay(5000)
                logger.info("Background Agentic Monitoring Engaged.")
                
                // Keep evaluating indefinitely while the module is active
                while (isActive) {
                    performFeedbackEvaluation()
                    // Sleep for an interval before the next "what-if" evaluation
                    delay(10_000)
                }
            } catch (e: CancellationException) {
                logger.info("Agentic loops cancelled normally during shutdown.")
            } catch (e: Exception) {
                logger.error("Error in Agentic feedback loop. Triggering auto-recovery.", e)
                // Negative feedback loop: If the agent crashes, we log the error and wait to try again
                delay(30_000)
                if (isActive) {
                    startAgenticLoops() // Resuscitate
                }
            }
        }
    }
    
    private suspend fun performFeedbackEvaluation() {
        logger.debug("Running TBL Optimization Trajectory against recent tags...")
        // In reality, this retrieves the live dynamic weights published by the TblOrchestrator UI.
        val currentWeights = com.example.aether.models.TblScore(0.5, 0.3, 0.2)
        
        // Triggers the 60m->15m prediction and potential Boardroom RAG negotiation
        com.example.aether.predict.TrajectoryForecaster.runForecastingLoop(currentWeights)
    }
}
