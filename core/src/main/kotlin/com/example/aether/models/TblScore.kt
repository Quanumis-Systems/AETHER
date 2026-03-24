package com.example.aether.models

/**
 * TblScore
 *
 * This data class defines the multi-objective optimization weightages 
 * for the Triple Bottom Line (TBL) Orchestrator.
 * It's structured to be immutable and thread-safe for the agentic loops.
 *
 * @property profitWeight Represents w1 (0.0 to 1.0)
 * @property peopleWeight Represents w2 (0.0 to 1.0)
 * @property planetWeight Represents w3 (0.0 to 1.0)
 */
data class TblScore(
    val profitWeight: Double = 0.33,
    val peopleWeight: Double = 0.33,
    val planetWeight: Double = 0.34
) {
    /**
     * Calculates the composite TBL Score based on the provided absolute metrics.
     * This acts as the Reward Function for the agentic negative feedback loop.
     */
    fun computeScore(
        profitMetric: Double, 
        peopleMetric: Double, 
        planetMetric: Double
    ): Double {
        return (profitWeight * profitMetric) + 
               (peopleWeight * peopleMetric) + 
               (planetWeight * planetMetric)
    }
}
