package com.example.aether.models

import java.util.UUID

/**
 * LedgerBlock
 *
 * A cryptographically signed block of high-fidelity telemetry data.
 * The block is composed of multiple PLC samples and is hashed 
 * to create an immutable audit trail for ESG reporting.
 *
 * @property blockId Unique identifier for this block of samples
 * @property timestamp Unix epoch timestamp of when the block was sealed
 * @property sampleCount Number of samples contained (e.g., 1000)
 * @property merkleRoot The BouncyCastle-generated SHA-256 Merkle root of the samples
 * @property previousBlockId Link to the previous ledger block, forming a chain
 */
data class LedgerBlock(
    val blockId: UUID = UUID.randomUUID(),
    val timestamp: Long = System.currentTimeMillis(),
    val sampleCount: Int,
    val merkleRoot: String,
    val previousBlockId: UUID?
)
