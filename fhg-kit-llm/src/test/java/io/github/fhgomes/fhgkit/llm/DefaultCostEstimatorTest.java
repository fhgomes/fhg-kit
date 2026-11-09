package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultCostEstimatorTest {

    private static final ModelRef MODEL_A = new ModelRef("p", "model-a");

    private final DefaultCostEstimator estimator = new DefaultCostEstimator(new InMemoryPriceTable(
            List.of(
                    new ModelPrice("model-a", Set.of("model-a-snapshot"), new BigDecimal("3.00"), new BigDecimal("15.00"), null),
                    ModelPrice.of("model-tiny", new BigDecimal("0.10"), new BigDecimal("0.40"))),
            "USD", "unit-test", LocalDate.of(2026, 1, 1)));

    @Test
    @DisplayName("should price one million input tokens at the per-Mtok price, scale 6")
    void shouldPriceOneMillionTokens() {
        var cost = estimator.estimate(MODEL_A, TokenUsage.exact(1_000_000, 0));

        assertThat(cost.status()).isEqualTo(CostEstimate.Status.ESTIMATED);
        assertThat(cost.isKnown()).isTrue();
        assertThat(cost.currency()).isEqualTo("USD");
        assertThat(cost.input()).isEqualTo(new BigDecimal("3.000000"));
        assertThat(cost.output()).isEqualTo(new BigDecimal("0.000000"));
        assertThat(cost.total()).isEqualTo(new BigDecimal("3.000000"));
        assertThat(cost.pricedAs()).isEqualTo("model-a");
    }

    @Test
    @DisplayName("should add input and output costs")
    void shouldAddBothSides() {
        var cost = estimator.estimate(MODEL_A, TokenUsage.exact(1_000, 500));

        assertThat(cost.input()).isEqualTo(new BigDecimal("0.003000"));
        assertThat(cost.output()).isEqualTo(new BigDecimal("0.007500"));
        assertThat(cost.total()).isEqualTo(new BigDecimal("0.010500"));
    }

    @Test
    @DisplayName("should round HALF_UP to scale 6")
    void shouldRoundHalfUp() {
        // 0.10 * 7 / 1_000_000 = 0.0000007 -> 0.000001 ; 0.40 * 1 / 1_000_000 = 0.0000004 -> 0.000000
        var cost = estimator.estimate(new ModelRef("p", "model-tiny"), TokenUsage.exact(7, 1));

        assertThat(cost.input()).isEqualTo(new BigDecimal("0.000001"));
        assertThat(cost.output()).isEqualTo(new BigDecimal("0.000000"));
        assertThat(cost.total()).isEqualTo(new BigDecimal("0.000001"));
        assertThat(cost.total().scale()).isEqualTo(DefaultCostEstimator.SCALE);
    }

    @Test
    @DisplayName("should report pricedAs as the canonical id when an alias matched")
    void shouldReportCanonicalIdForAlias() {
        var cost = estimator.estimate(new ModelRef("p", "MODEL-A-SNAPSHOT"), TokenUsage.exact(10, 10));

        assertThat(cost.status()).isEqualTo(CostEstimate.Status.ESTIMATED);
        assertThat(cost.pricedAs()).isEqualTo("model-a");
    }

    @Test
    @DisplayName("should answer UNKNOWN_MODEL with zero amounts when the table has no entry")
    void shouldAnswerUnknownModel() {
        var cost = estimator.estimate(new ModelRef("p", "model-a-v2"), TokenUsage.exact(1_000_000, 1_000_000));

        assertThat(cost.status()).isEqualTo(CostEstimate.Status.UNKNOWN_MODEL);
        assertThat(cost.isKnown()).isFalse();
        assertThat(cost.input()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cost.output()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cost.total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cost.total().scale()).isEqualTo(DefaultCostEstimator.SCALE);
        assertThat(cost.pricedAs()).isNull();
    }

    @Test
    @DisplayName("should answer UNKNOWN_TOKENS when tokens are estimated as zero, but ESTIMATED when exactly zero")
    void shouldDistinguishUnknownTokensFromFree() {
        var unknown = estimator.estimate(MODEL_A, TokenUsage.estimated(0, 0));
        var free = estimator.estimate(MODEL_A, TokenUsage.zero());
        var estimatedNonZero = estimator.estimate(MODEL_A, TokenUsage.estimated(1_000_000, 0));

        assertThat(unknown.status()).isEqualTo(CostEstimate.Status.UNKNOWN_TOKENS);
        assertThat(unknown.total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(unknown.pricedAs()).isEqualTo("model-a");
        assertThat(free.status()).isEqualTo(CostEstimate.Status.ESTIMATED);
        assertThat(free.total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(estimatedNonZero.status()).isEqualTo(CostEstimate.Status.ESTIMATED);
        assertThat(estimatedNonZero.total()).isEqualTo(new BigDecimal("3.000000"));
    }

    @Test
    @DisplayName("should price with an empty table as UNKNOWN_MODEL")
    void shouldHandleEmptyTable() {
        var empty = new DefaultCostEstimator(InMemoryPriceTable.empty("EUR"));

        var cost = empty.estimate(MODEL_A, TokenUsage.exact(1, 1));

        assertThat(cost.status()).isEqualTo(CostEstimate.Status.UNKNOWN_MODEL);
        assertThat(cost.currency()).isEqualTo("EUR");
    }

    private static final ModelRef SONNET = new ModelRef("p", "claude-sonnet-4");

    private static DefaultCostEstimator sonnetEstimator(BigDecimal cacheRead, BigDecimal cacheWrite) {
        return new DefaultCostEstimator(new InMemoryPriceTable(
                List.of(new ModelPrice("claude-sonnet-4", Set.of(), new BigDecimal("3.00"), new BigDecimal("15.00"),
                        null, cacheRead, cacheWrite)),
                "USD", "unit-test", LocalDate.of(2026, 1, 1)));
    }

    @Test
    @DisplayName("should price a cached call at the list rates: 0.015675, not 0.1095 (spec 004 US1)")
    void shouldPriceCachedCallAtCacheRates() {
        var est = sonnetEstimator(new BigDecimal("0.30"), new BigDecimal("3.75"));

        var cost = est.estimate(SONNET, TokenUsage.exact(36_000, 100, 35_000, 900));

        // input side: 100 x 3.00 + 35 000 x 0.30 + 900 x 3.75 = 300 + 10 500 + 3 375 = 14 175 per 1e6 = 0.014175
        assertThat(cost.status()).isEqualTo(CostEstimate.Status.ESTIMATED);
        assertThat(cost.input()).isEqualTo(new BigDecimal("0.014175"));
        assertThat(cost.output()).isEqualTo(new BigDecimal("0.001500"));
        assertThat(cost.total()).isEqualTo(new BigDecimal("0.015675"));
    }

    @Test
    @DisplayName("should price the same call without a split at the full input rate: 0.1095")
    void shouldPriceWithoutSplitAtFullInputRate() {
        var est = sonnetEstimator(new BigDecimal("0.30"), new BigDecimal("3.75"));

        var cost = est.estimate(SONNET, TokenUsage.exact(36_000, 100));

        assertThat(cost.total()).isEqualTo(new BigDecimal("0.109500"));
    }

    @Test
    @DisplayName("should fall back to the input rate for an absent cache price, never cheaper by guess")
    void shouldFallBackToInputRateWhenCachePriceAbsent() {
        var est = sonnetEstimator(null, null);

        var cost = est.estimate(SONNET, TokenUsage.exact(36_000, 100, 35_000, 900));

        assertThat(cost.total()).isEqualTo(new BigDecimal("0.109500"));
    }

    @Test
    @DisplayName("should use each cache rate on its own when only one is declared")
    void shouldFallBackPerRate() {
        var est = sonnetEstimator(new BigDecimal("0.30"), null);

        var cost = est.estimate(SONNET, TokenUsage.exact(36_000, 100, 35_000, 900));

        // 100 x 3 + 35 000 x 0.30 + 900 x 3.00 = 300 + 10 500 + 2 700 = 13 500 ; + output 1 500
        assertThat(cost.total()).isEqualTo(new BigDecimal("0.015000"));
    }

    @Test
    @DisplayName("should price an event without cache fields exactly as in 0.2.0, whatever the cache rates (spec 004 US2)")
    void shouldPriceOldEventsAsBefore() {
        var est = sonnetEstimator(new BigDecimal("0.30"), new BigDecimal("3.75"));

        var cost = est.estimate(SONNET, TokenUsage.exact(1_000, 500));

        assertThat(cost.input()).isEqualTo(new BigDecimal("0.003000"));
        assertThat(cost.output()).isEqualTo(new BigDecimal("0.007500"));
        assertThat(cost.total()).isEqualTo(new BigDecimal("0.010500"));
    }
}
