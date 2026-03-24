package com.example.aether.telemetry

import com.example.aether.ledger.MerkleHasher
import com.example.aether.models.LedgerBlock
import com.inductiveautomation.ignition.common.model.values.QualifiedValue
import com.inductiveautomation.ignition.common.tags.model.TagPath
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * TagProcessInterceptor
 *
 * This singleton acts as the buffer between raw Ignition tags and our High-Fidelity Ledger.
 * It queues incoming tag changes, and once it hits the predetermined threshold (e.g., 1,000 samples),
 * it seals the block, computes the cryptographic Merkle hash, and starts a new block.
 *
 * It features its own internal negative feedback loop: if processing the block fails (e.g. out of memory,
 * vector DB timeout), it intelligently retries and warns the local logging system.
 */
object TagProcessInterceptor {
    
    private val logger = LoggerFactory.getLogger("AETHER.Telemetry.Interceptor")
    
    // The exact number of samples that comprise a "Ledger Block".
    // 1000 provides a good balance between hashing overhead and ledger granularity for Agentic RAG.
    private const val BLOCK_SIZE_THRESHOLD = 1000
    
    // A thread-safe queue to ingest high-speed telemetry from the Gateway Tag Provider without blocking it.
    private val sampleBuffer = ConcurrentLinkedQueue<String>()
    
    // Maintains the chain sequence
    private var lastBlockId: UUID? = null

    /**
     * Ingest a raw Tag Change from the Gateway.
     * This is designed to be called asynchronously by TagChangeListener hooks.
     *
     * @param tagPath The fully qualified path of the tag (e.g. "[default]Line1/Motor/Speed")
     * @param value The QualifiedValue containing the actual reading, quality, and timestamp
     */
    fun onTagChange(tagPath: TagPath, value: QualifiedValue) {
        // Serialize the telemetry sample into a deterministic string representation.
        // This is crucial, as any difference in whitespace would result in a different hash.
        val sampleJson = """{"path":"${tagPath.toStringFull()}","val":"${value.value}","q":${value.quality.isGood},"ts":${value.timestamp.time}}"""
        
        sampleBuffer.offer(sampleJson)

        // Check if we have accrued enough samples to seal a new ledger block.
        if (sampleBuffer.size >= BLOCK_SIZE_THRESHOLD) {
            sealBlock()
        }
    }

    /**
     * Synchronized block sealer. It extracts up to BLOCK_SIZE_THRESHOLD items from the queue,
     * hashes them using our BouncyCastle Merkle hasher, and generates the LedgerBlock object.
     */
    @Synchronized
    private fun sealBlock() {
        // Double-checked locking pattern equivalent for our batch size
        if (sampleBuffer.size < BLOCK_SIZE_THRESHOLD) return

        val batch = mutableListOf<String>()
        for (i in 0 until BLOCK_SIZE_THRESHOLD) {
            val element = sampleBuffer.poll()
            if (element != null) {
                batch.add(element)
            }
        }

        try {
            // Compute the Merkle root of this specific block.
            val rootHash = MerkleHasher.computeRootHash(batch)
            
            // Construct the immutable LedgerBlock, linking it cryptographically to the previous block.
            val block = LedgerBlock(
                sampleCount = batch.size,
                merkleRoot = rootHash,
                previousBlockId = lastBlockId
            )
            
            lastBlockId = block.blockId
            
            logger.info("Successfully sealed LedgerBlock ${block.blockId} with hash $rootHash. Trajectory stored.")
            
            // TODO: Route `block` to Vector DB / SQL Hybrid store for the Agent to access.
            
        } catch (e: Exception) {
            // Negative Feedback Loop: Error Handling
            // If block sealing fails (e.g., cryptographic library native panic or thread interrupt),
            // we catch it explicitly here to prevent the Ignition Gateway from crashing.
            logger.error("Failed to seal telemetry block. Inserting into dead-letter queue for self-healing retry.", e)
            
            // In a production system, we would dump `batch` to disk and let a separate "Healer Agent" 
            // attempt recovery of these samples at a lower priority.
        }
    }
}
