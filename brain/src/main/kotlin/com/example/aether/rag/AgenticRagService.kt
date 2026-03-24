package com.example.aether.rag

import com.example.aether.models.LedgerBlock
import com.example.aether.predict.OnnxPredictor
import com.example.aether.models.TblScore
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import org.slf4j.LoggerFactory

/**
 * AgenticRagService
 *
 * This singleton forms the "Intelligence Layer" of AETHER.
 * It manages the interactions between raw telemetry (Vectorized Text Segments)
 * and the Local Large Language Model (e.g. LLaMA 3 or Mistral running on a GPU Gateway).
 *
 * Beginner Guide:
 * 1. "Tag Strings" don't mean much to an LLM. We generate a semantic mapping.
 * 2. We use an Embedding Model (`AllMiniLmL6V2EmbeddingModel`) to turn that text into an array of floats (embeddings).
 * 3. We store that in Qdrant (our Vector Database).
 * 4. When an anomalous event happens, the Agent asks Qdrant "What did we do last time this happened?"
 * 5. Qdrant returns the historical "state-action" pairs.
 * 6. The LLM combines the history, the current ONNX trajectory, and the TBL weights to propose a new setpoint.
 */
object AgenticRagService {
    
    private val logger = LoggerFactory.getLogger("AETHER.RAG")
    
    // In a production setup, this would point to a local Ollama or vLLM instance to maintain
    // the "Local-First Execution" requirement (no data leaves the factory).
    private val chatModel: ChatLanguageModel by lazy {
        OpenAiChatModel.builder()
            .apiKey("LOCAL_LLM_KEY") // Standard adapter for local OpenAI-compatible endpoints
            .baseUrl("http://localhost:11434/v1") // Ollama default port
            .modelName("mistral")
            .temperature(0.2) // Low temperature for deterministic industrial advice
            .build()
    }
    
    // We use a small local embedding model directly in Java/Kotlin via ONNX Runtime!
    private val embeddingModel: EmbeddingModel = AllMiniLmL6V2EmbeddingModel()
    
    // Represents our connection to the Milvus or Qdrant Vector Store
    private val vectorStore = QdrantEmbeddingStore.builder()
        .collectionName("AETHER_TELEMETRY")
        .host("localhost")
        .port(6334)
        .build()
        
    // Tracks consecutive Agent hallucinations or Vector Store timeouts
    private var agentErrorScore = 0

    /**
     * Translates a new Ledger Block into Semantic Text and ingests it into Qdrant.
     * This creates the historical "State-Action" embeddings for the RAG loop.
     */
    fun ingestLedgerBlock(block: LedgerBlock, semanticDescription: String) {
        try {
            // Turn the human-readable description of the machine state into a Text Segment
            val segment = TextSegment.from(semanticDescription)
            
            // Generate the semantic vector embeddings
            val embedding = embeddingModel.embed(segment).content()
            
            // Store it in the Vector Database with the Ledger Block ID as metadata
            vectorStore.add(embedding, segment)
            
            logger.info("LedgerBlock ${block.blockId} mathematically embedded and stored in Qdrant.")
            
            // Auto-heal the error score upon success
            if (agentErrorScore > 0) agentErrorScore--
            
        } catch (e: Exception) {
            // Negative Feedback Loop: If the vector store is down or we OOM,
            // we increment the error score. If it goes too high, the Agent shuts itself off
            // from making autonomous decisions to prevent blind-piloting.
            agentErrorScore += 2
            logger.error("Failed to vectorize LedgerBlock. Semantic memory ingestion paused.", e)
            if (agentErrorScore > 10) {
                logger.error("CRITICAL: Vector Store degraded. Agentic Recommendations disabled.")
            }
        }
    }

    /**
     * The core "What-If" Simulation Loop.
     * Evaluates the multi-objective TBL function alongside the ONNX prediction.
     */
    suspend fun evaluateTrajectoryAgainstTbl(
        currentTblWeights: TblScore,
        currentTagStates: List<String>
    ): String {
        if (agentErrorScore > 10) {
            return "AGENT OFFLINE: Safety interlock engaged due to sub-system degradation."
        }
        
        try {
            // 1. Run the lightweight ONNX projection to find out where we are heading
            // (Mock feature array for example purposes)
            val trajectoryFeatureArray = FloatArray(10) { 0.5f }
            val futureOutput = OnnxPredictor.predictFutureState(trajectoryFeatureArray)
            
            // 2. Query Qdrant for similar states in the past (Retrieval)
            val currentStateDescription = "Factory is operating at setpoints. Current prediction: $futureOutput"
            val queryEmbedding = embeddingModel.embed(currentStateDescription).content()
            val relevantHistory = vectorStore.findRelevant(queryEmbedding, 3, 0.7)
            
            // 3. Construct the prompt for the Local LLM (Augmentation)
            val prompt = """
                You are AETHER, an industrial ESG agent.
                Current Priority Weights - Profit: ${currentTblWeights.profitWeight}, People: ${currentTblWeights.peopleWeight}, Planet: ${currentTblWeights.planetWeight}
                60-Min Trajectory Forecast: $futureOutput
                
                Historical contexts when we were in similar states:
                ${relevantHistory.joinToString("\n") { it.textSegment().text() }}
                
                Question: Based on this trajectory, our TBL weights, and past results, what VFD or HVAC setpoints should we adjust?
                Provide exactly 1 recommendation.
            """.trimIndent()
            
            // 4. Generate the conclusion (Generation)
            val aiResponse = chatModel.generate(prompt)
            return aiResponse
            
        } catch (e: Exception) {
            agentErrorScore++
            logger.error("Agentic What-If Simulation Failed. Aborting autonomous control recommendation.", e)
            return "ERROR: Simulation failed. Awaiting human intervention."
        }
    }
}
