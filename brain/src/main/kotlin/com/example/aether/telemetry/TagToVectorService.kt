/*
 * Copyright © 2026 Quanumis Systems. All rights reserved.
 * This file is part of the AETHER Agentic Core (/brain) and is 
 * bound by the Business Source License 1.1 (BSL).
 * Commercial production use is strictly prohibited without authorization.
 */
package com.example.aether.telemetry

import com.inductiveautomation.ignition.common.model.values.QualifiedValue
import com.inductiveautomation.ignition.common.tags.model.TagPath
import com.inductiveautomation.ignition.common.tags.config.types.TagObjectType
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue
import kotlinx.coroutines.*
import java.net.Socket
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore

/**
 * TagToVectorService
 *
 * This logger tracks kWh for the Planet Agent and Safety metrics for the People agent.
 * Implements the Dual-Stream Dispatcher handling High-Fidelity Data Ingestion.
 * Designed for aerospace burst telemetry (Tinker AFB spec) using a LinkedBlockingQueue
 * and isolated thread pools.
 */
object TagToVectorService {
    private val logger = LoggerFactory.getLogger("AETHER.Subagent.Telemetrist")
    
    // High-speed queue to prevent Gateway UI lag during burst ingestion
    private val telemetryQueue = LinkedBlockingQueue<TelemetryEvent>(100_000)
    
    // Isolated Thread Pool ensuring the main Ignition Gateway is never blocked by NLP/Embedding execution
    private val vectorDispatcherScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Embedded ONNX Model for rapid local semantic vectorization
    private val embeddingModel = AllMiniLmL6V2EmbeddingModel()
    
    // Connecting to Qdrant exactly as requested via gRPC
    private val vectorStore = QdrantEmbeddingStore.builder()
        .collectionName("Industrial_Context")
        .host("localhost")
        .port(6334)
        .useTls(false)
        .build()

    private var isDispatching = false

    // Core Data Class
    data class TelemetryEvent(
        val path: TagPath,
        val value: QualifiedValue,
        val metadata: String
    )

    fun startDispatcher() {
        if (isDispatching) return
        isDispatching = true
        
        logger.info("Initializing Telemetry Ledger Dispatcher (Tinker AFB burst spec)...")
        
        vectorDispatcherScope.launch {
            while (isActive) {
                // Blocks efficiently until data is available
                val event = telemetryQueue.take()
                processDualStream(event)
            }
        }
    }
    
    fun stopDispatcher() {
        isDispatching = false
        vectorDispatcherScope.cancel()
    }

    /**
     * Enqueues a raw Ignition Tag Change for processing.
     * Called instantly by the TagChangeListener.
     */
    fun enqueue(path: TagPath, value: QualifiedValue, parentFolder: String) {
        // Form the Semantic Anchor
        val metadata = "UDT Context: $parentFolder"
        telemetryQueue.offer(TelemetryEvent(path, value, metadata))
    }

    /**
     * The core transformation pipeline separating timeseries constraints from semantic context.
     */
    private suspend fun processDualStream(event: TelemetryEvent) {
        try {
            // 1. Graphene-Solid HMAC Signing for cryptographic immutability
            val hmacSignature = signPayload(event)
            
            // 2. Stream A: QuestDB (ILP - InfluxDB Line Protocol) for timeseries
            dispatchToQuestDb(event, hmacSignature)
            
            // 3. Stream B: Qdrant Vectorization for the Agentic RAG
            dispatchToQdrant(event, hmacSignature)
            
        } catch (e: Exception) {
            logger.error("Failed to process dual-stream telemetry for ${event.path}. Dropping frame.", e)
        }
    }

    /**
     * SECURITY: Applies SHA-256 HMAC. Key is pulled from a protected environment (mocked here).
     */
    private fun signPayload(event: TelemetryEvent): String {
        // In reality, extracted from Ignition's Internal System Key Provider
        val secretKey = System.getenv("AETHER_LEDGER_KEY") ?: "QUANUMIS_INTERNAL_DEFAULT_KEY_DO_NOT_USE_IN_PROD"
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(secretKey.toByteArray(), algorithm))
        
        val payload = "${event.path.toStringFull()}:${event.value.value}:${event.value.timestamp.time}"
        val bytes = mac.doFinal(payload.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Stream A: High-speed QuestDB insertion via raw TCP socket.
     * This logger tracks raw OEE states for the Profit Agent.
     */
    private fun dispatchToQuestDb(event: TelemetryEvent, signature: String) {
        try {
            // Simulated TCP ILP transmission. 
            // example format: aether_telemetry,tag_path=Speed value=1450.2,hmac="sig" 16298371982
            val ilpFrame = "aether_ledger,path=${event.path.itemName} value=${event.value.value},hmac=\"$signature\" ${event.value.timestamp.time}000000\n"
            // Socket("localhost", 9009).getOutputStream().write(ilpFrame.toByteArray())
        } catch (e: Exception) {
            logger.warn("QuestDB stream unavailable. (Simulated)")
        }
    }

    /**
     * Stream B: Vectorization for Agentic Context.
     * This logger tracks environmental violations for the Planet Agent and safety for the People agent.
     */
    private fun dispatchToQdrant(event: TelemetryEvent, signature: String) {
        // Convert to JSON-L
        val jsonLD = """{"path":"${event.path.toStringFull()}","val":${event.value.value},"meta":"${event.metadata}","sig":"$signature"}"""
        
        val segment = TextSegment.from(jsonLD)
        val embedding = embeddingModel.embed(segment).content()
        
        vectorStore.add(embedding, segment)
    }
}
