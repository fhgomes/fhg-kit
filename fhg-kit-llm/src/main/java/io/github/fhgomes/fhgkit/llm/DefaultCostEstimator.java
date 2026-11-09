package io.github.fhgomes.fhgkit.llm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * {@link CostEstimator} that applies per-million-token prices from a {@link PriceTable}.
 *
 * <p>Arithmetic is {@link BigDecimal} at scale {@value #SCALE} with {@link RoundingMode#HALF_UP}:
 * {@code cost = price * tokens / 1_000_000}, rounded once per side. On the input side the tokens that
 * {@link TokenUsage#cacheRead() were read from} or {@link TokenUsage#cacheWrite() written to} the prompt cache are
 * priced at {@link ModelPrice#effectiveCacheReadPerMtok()} / {@link ModelPrice#effectiveCacheWritePerMtok()}
 * (the input rate when the price declares no cache rate) and the rest at the input rate. Statuses:
 * <ul>
 *   <li>{@link CostEstimate.Status#UNKNOWN_MODEL} when the table has no entry for the model;</li>
 *   <li>{@link CostEstimate.Status#UNKNOWN_TOKENS} when the tokens are {@link TokenUsage#estimated() estimated}
 *       and total zero, meaning the application did not know how many were used;</li>
 *   <li>{@link CostEstimate.Status#ESTIMATED} otherwise, with {@link CostEstimate#pricedAs()} set to the id of
 *       the matched {@link ModelPrice} (which may differ from the requested model id when an alias matched).</li>
 * </ul>
 *
 * <p>Stateless and thread-safe as long as the table is.
 */
public final class DefaultCostEstimator implements CostEstimator {

    /** Scale of every amount produced. Six decimals keep sub-cent costs of small calls visible. */
    public static final int SCALE = 6;

    private static final BigDecimal TOKENS_PER_MTOK = BigDecimal.valueOf(1_000_000L);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY);

    private final PriceTable table;

    /** @param table the prices to apply */
    public DefaultCostEstimator(PriceTable table) {
        this.table = Objects.requireNonNull(table, "table");
    }

    @Override
    public CostEstimate estimate(ModelRef model, TokenUsage tokens) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(tokens, "tokens");
        var price = table.lookup(model);
        if (price.isEmpty()) {
            return new CostEstimate(CostEstimate.Status.UNKNOWN_MODEL, table.currency(), ZERO, ZERO, ZERO, null);
        }
        var matched = price.get();
        if (tokens.estimated() && tokens.total() == 0) {
            return new CostEstimate(
                    CostEstimate.Status.UNKNOWN_TOKENS, table.currency(), ZERO, ZERO, ZERO, matched.id());
        }
        BigDecimal input = inputCost(matched, tokens);
        BigDecimal output = cost(matched.outputPerMtok(), tokens.output());
        return new CostEstimate(
                CostEstimate.Status.ESTIMATED, table.currency(), input, output, input.add(output), matched.id());
    }

    /**
     * Input side: the uncached part at the input rate, the cache parts at their rates (an absent cache rate means
     * the input rate), summed before the single rounding. Without a cache split this is the 0.2.0 formula.
     */
    private static BigDecimal inputCost(ModelPrice price, TokenUsage tokens) {
        BigDecimal sum = price.inputPerMtok().multiply(BigDecimal.valueOf(tokens.uncachedInput()))
                .add(price.effectiveCacheReadPerMtok().multiply(BigDecimal.valueOf(tokens.cacheRead())))
                .add(price.effectiveCacheWritePerMtok().multiply(BigDecimal.valueOf(tokens.cacheWrite())));
        return sum.divide(TOKENS_PER_MTOK, SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal cost(BigDecimal pricePerMtok, int tokens) {
        return pricePerMtok.multiply(BigDecimal.valueOf(tokens)).divide(TOKENS_PER_MTOK, SCALE, RoundingMode.HALF_UP);
    }

    /** The table this estimator prices against. */
    public PriceTable table() {
        return table;
    }
}
