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
 */
public record ModelPrice(
        String id,
        Set<String> aliases,
        BigDecimal inputPerMtok,
        BigDecimal outputPerMtok,
        @Nullable Integer contextWindow) {

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
    }

    /** Convenience for a price without aliases or context window. */
    public static ModelPrice of(String id, BigDecimal inputPerMtok, BigDecimal outputPerMtok) {
        return new ModelPrice(id, Set.of(), inputPerMtok, outputPerMtok, null);
    }
}
