package io.github.fhgomes.fhgkit.llm;

/**
 * Token counts of one model call.
 *
 * <p>{@code input} is always the <b>whole</b> prompt. The cache parts are <i>parts of it</i>, not additions:
 * {@code cacheRead + cacheWrite <= input}, and {@link #uncachedInput()} is what remains. A caller that does not know
 * the split leaves both at zero, which prices the whole prompt at the input rate.
 *
 * @param input      prompt (input) tokens, the whole prompt including the cache parts, never negative
 * @param output     completion (output) tokens, never negative
 * @param estimated  {@code true} when the counts were estimated by the application (for example from text
 *                   length) rather than reported by the provider
 * @param cacheRead  part of {@code input} served from the provider's prompt cache, never negative
 * @param cacheWrite part of {@code input} written to the provider's prompt cache, never negative
 */
public record TokenUsage(int input, int output, boolean estimated, int cacheRead, int cacheWrite) {

    private static final TokenUsage ZERO = new TokenUsage(0, 0, false);

    /**
     * Validates the counts.
     *
     * @throws IllegalArgumentException when a count is negative or {@code cacheRead + cacheWrite} exceeds
     *                                  {@code input}
     */
    public TokenUsage {
        Checks.requireNonNegative(input, "input");
        Checks.requireNonNegative(output, "output");
        Checks.requireNonNegative(cacheRead, "cacheRead");
        Checks.requireNonNegative(cacheWrite, "cacheWrite");
        if ((long) cacheRead + cacheWrite > input) {
            throw new IllegalArgumentException("cacheRead + cacheWrite must not exceed input, got "
                    + cacheRead + " + " + cacheWrite + " > " + input);
        }
    }

    /** Counts without a cache split (both cache parts zero); what 0.1 and 0.2 callers use. */
    public TokenUsage(int input, int output, boolean estimated) {
        this(input, output, estimated, 0, 0);
    }

    /** Returns {@code input + output}; the cache parts are inside {@code input} and are not added again. */
    public int total() {
        return input + output;
    }

    /** The part of {@code input} that was neither read from nor written to the cache. */
    public int uncachedInput() {
        return input - cacheRead - cacheWrite;
    }

    /** Exact zero usage: what failures are recorded with when nothing was consumed. */
    public static TokenUsage zero() {
        return ZERO;
    }

    /** Counts reported by the provider, without a cache split. */
    public static TokenUsage exact(int input, int output) {
        return new TokenUsage(input, output, false);
    }

    /** Counts reported by the provider, with the cache split ({@code input} is the whole prompt). */
    public static TokenUsage exact(int input, int output, int cacheRead, int cacheWrite) {
        return new TokenUsage(input, output, false, cacheRead, cacheWrite);
    }

    /** Counts estimated by the application. {@code estimated(0, 0)} means "unknown", not "free". */
    public static TokenUsage estimated(int input, int output) {
        return new TokenUsage(input, output, true);
    }
}
