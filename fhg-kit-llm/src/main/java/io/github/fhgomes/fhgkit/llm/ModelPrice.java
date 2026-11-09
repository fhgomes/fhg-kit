package io.github.fhgomes.fhgkit.llm;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * List price of one model, per million tokens.
 *
 * <p>Both sides of the price are required: a model priced on input only cannot be estimated, so the record
 * rejects it instead of silently producing a partial cost. Ids and aliases are trimmed; matching in a
 * {@link PriceTable} is case-insensitive and exact (never by prefix).
 *
 * @param id            canonical model id, as the provider names it; never blank
 * @param aliases       other ids that resolve to this price (dated snapshots, gateway names); never null, may be
 *                      empty, no blank entries
 * @param inputPerMtok  price of one million input tokens, never negative
 * @param outputPerMtok price of one million output tokens, never negative
 * @param contextWindow maximum context size in tokens, when known; positive
 * @param cacheReadPerMtok  price of one million input tokens served from the prompt cache, or {@code null} when
 *                          not declared (then the input rate applies, see {@link #effectiveCacheReadPerMtok()})
 * @param cacheWritePerMtok price of one million input tokens written to the prompt cache, or {@code null} when
 *                          not declared (then the input rate applies, see {@link #effectiveCacheWritePerMtok()})
 */
public record ModelPrice(
        String id,
        Set<String> aliases,
        BigDecimal inputPerMtok,
        BigDecimal outputPerMtok,
        @Nullable Integer contextWindow,
        @Nullable BigDecimal cacheReadPerMtok,
        @Nullable BigDecimal cacheWritePerMtok) {

    /**
     * Validates and normalizes the entry.
     *
     * @throws IllegalArgumentException when the id or an alias is blank, when either side of the price is missing
     *                                  or negative, or when the context window is not positive
     */
    public ModelPrice {
        id = Checks.requireNonBlank(id, "id");
        Objects.requireNonNull(aliases, "aliases");
        var normalizedAliases = new LinkedHashSet<String>();
        for (String alias : aliases) {
            normalizedAliases.add(Checks.requireNonBlank(alias, "alias of " + id));
        }
        aliases = Set.copyOf(normalizedAliases);
        // Both sides are declared non-null, but a consumer mapping configuration may still pass null through.
        // Reject it as an argument error with the model id, which is what the operator needs to fix it.
        if (inputPerMtok == null || outputPerMtok == null) {
            throw new IllegalArgumentException(
                    "price of " + id + " must define both inputPerMtok and outputPerMtok");
        }
        Checks.requireNonNegative(inputPerMtok, "inputPerMtok of " + id);
        Checks.requireNonNegative(outputPerMtok, "outputPerMtok of " + id);
        if (contextWindow != null && contextWindow <= 0) {
            throw new IllegalArgumentException("contextWindow of " + id + " must be positive, got " + contextWindow);
        }
        if (cacheReadPerMtok != null) {
            Checks.requireNonNegative(cacheReadPerMtok, "cacheReadPerMtok of " + id);
        }
        if (cacheWritePerMtok != null) {
            Checks.requireNonNegative(cacheWritePerMtok, "cacheWritePerMtok of " + id);
        }
    }

    /** Price without cache rates (the 0.1 and 0.2 shape): cache tokens are priced at the input rate. */
    public ModelPrice(
            String id,
            Set<String> aliases,
            BigDecimal inputPerMtok,
            BigDecimal outputPerMtok,
            @Nullable Integer contextWindow) {
        this(id, aliases, inputPerMtok, outputPerMtok, contextWindow, null, null);
    }

    /** Rate for cache-read tokens: the declared one, or the input rate when none is declared (never cheaper by guess). */
    public BigDecimal effectiveCacheReadPerMtok() {
        return cacheReadPerMtok != null ? cacheReadPerMtok : inputPerMtok;
    }

    /** Rate for cache-write tokens: the declared one, or the input rate when none is declared (never cheaper by guess). */
    public BigDecimal effectiveCacheWritePerMtok() {
        return cacheWritePerMtok != null ? cacheWritePerMtok : inputPerMtok;
    }

    /** A copy of this price with the given cache rates ({@code null} = not declared). */
    public ModelPrice withCacheRates(@Nullable BigDecimal cacheReadPerMtok, @Nullable BigDecimal cacheWritePerMtok) {
        return new ModelPrice(id, aliases, inputPerMtok, outputPerMtok, contextWindow, cacheReadPerMtok,
                cacheWritePerMtok);
    }

    /** Convenience for a price without aliases or context window. */
    public static ModelPrice of(String id, BigDecimal inputPerMtok, BigDecimal outputPerMtok) {
        return new ModelPrice(id, Set.of(), inputPerMtok, outputPerMtok, null);
    }
}
