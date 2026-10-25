package io.github.fhgomes.fhgkit.llm;

/**
 * Token counts of one model call.
 *
 * @param input     prompt (input) tokens, never negative
 * @param output    completion (output) tokens, never negative
 * @param estimated {@code true} when the counts were estimated by the application (for example from text
 *                  length) rather than reported by the provider
 */
public record TokenUsage(int input, int output, boolean estimated) {

    private static final TokenUsage ZERO = new TokenUsage(0, 0, false);

    /**
     * Validates the counts.
     *
     * @throws IllegalArgumentException when a count is negative
     */
    public TokenUsage {
        Checks.requireNonNegative(input, "input");
        Checks.requireNonNegative(output, "output");
    }

    /** Returns {@code input + output}. */
    public int total() {
        return input + output;
    }

    /** Exact zero usage: what failures are recorded with when nothing was consumed. */
    public static TokenUsage zero() {
        return ZERO;
    }

    /** Counts reported by the provider. */
    public static TokenUsage exact(int input, int output) {
        return new TokenUsage(input, output, false);
    }

    /** Counts estimated by the application. {@code estimated(0, 0)} means "unknown", not "free". */
    public static TokenUsage estimated(int input, int output) {
        return new TokenUsage(input, output, true);
    }
}
