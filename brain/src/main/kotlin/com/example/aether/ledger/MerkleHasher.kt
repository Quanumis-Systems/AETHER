package com.example.aether.ledger

import org.bouncycastle.util.encoders.Hex
import java.security.MessageDigest
import java.security.Security

/**
 * MerkleHasher
 *
 * This utility class is responsible for computing the cryptographic root of a "High-Fidelity Ledger Block".
 * In aerospace and defense (ISA/IEC 62443 compliance), maintaining a verifiable audit trail of telemetry 
 * ensures that historical state is tamper-proof—critical for Agentic RAG confidence.
 *
 * We implement a standard Merkle Tree using BouncyCastle's SHA-256 digest.
 */
object MerkleHasher {
    
    init {
        // Register BouncyCastleProvider natively to securely hash our telemetry blocks.
        // BouncyCastle is a reliable, industry-standard cryptographic library.
        Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
    }

    /**
     * Hashes a list of telemetry samples into a single Merkle Tree Root Hex String.
     * 
     * How it works (Agentic Negative Feedback loop resilient):
     * 1. If the list is empty, return a zero-hash.
     * 2. Hashes every individual sample string.
     * 3. Pairs them up and hashes the concatenations until only one root hash remains.
     * 4. If a block fails validation in the future, the agent can traverse this tree to find exactly WHICH tag was compromised.
     *
     * @param samples A list of serialized tag change events (e.g., JSON strings containing tag path, value, quality, timestamp)
     * @return The SHA-256 Hex String of the Merkle Root
     */
    fun computeRootHash(samples: List<String>): String {
        if (samples.isEmpty()) return "0000000000000000000000000000000000000000000000000000000000000000"

        // Step 1: Compute initial leaf hashes
        var currentLevelHashes = samples.map { hashString(it) }

        // Step 2: Iteratively hash pairs until we reach the root
        while (currentLevelHashes.size > 1) {
            val nextLevel = mutableListOf<String>()
            
            // Step through pairs (step by 2).
            for (i in currentLevelHashes.indices step 2) {
                val left = currentLevelHashes[i]
                // If there's an odd number of hashes, we duplicate the last one to pair it with itself.
                // This is a standard Merkle Tree balancing technique.
                val right = if (i + 1 < currentLevelHashes.size) currentLevelHashes[i + 1] else left
                
                nextLevel.add(hashString(left + right))
            }
            currentLevelHashes = nextLevel
        }

        // The final element remaining is the unbreakable root of our telemetry block.
        return currentLevelHashes[0]
    }

    /**
     * Helper to compute the SHA-256 hash of a single string using BouncyCastle.
     */
    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256", "BC")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return String(Hex.encode(hashBytes))
    }
}
