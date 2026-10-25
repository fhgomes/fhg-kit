package io.github.fhgomes.fhgkit.llm;

/**
 * Port: turns token counts into money.
 *
 * <p>The kit ships {@link DefaultCostEstimator}. Implementations must never throw for an unknown model or
 * unknown tokens; they answer with the matching {@link CostEstimate.Status} instead, so the caller can store the
 * event and price it later.
 */
public interface CostEstimator {

    /**
     * Estimates the cost of one call.
     *
     * @param model  the model that served the call
     * @param tokens the tokens it consumed
     * @return an estimate; never null
     */
    CostEstimate estimate(ModelRef model, TokenUsage tokens);
}
