package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModelPriceTest {

    @Test
    @DisplayName("should trim id and aliases")
    void shouldTrimIdAndAliases() {
        var price = new ModelPrice(" model-a ", Set.of(" alias-1 "), BigDecimal.ONE, BigDecimal.TEN, 8192);

        assertThat(price.id()).isEqualTo("model-a");
        assertThat(price.aliases()).containsExactly("alias-1");
        assertThat(price.contextWindow()).isEqualTo(8192);
    }

    @Test
    @DisplayName("should reject a price with one side missing")
    void shouldRejectOneSidedPrice() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ModelPrice("model-a", Set.of(), null, BigDecimal.ONE, null))
                .withMessageContaining("model-a").withMessageContaining("inputPerMtok");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ModelPrice("model-a", Set.of(), BigDecimal.ONE, null, null))
                .withMessageContaining("outputPerMtok");
    }

    @Test
    @DisplayName("should reject negative prices, blank aliases and non-positive context window")
    void shouldRejectInvalidValues() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ModelPrice.of("m", new BigDecimal("-1"), BigDecimal.ONE));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ModelPrice("m", Set.of(" "), BigDecimal.ONE, BigDecimal.ONE, null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ModelPrice("m", Set.of(), BigDecimal.ONE, BigDecimal.ONE, 0));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ModelPrice.of("", BigDecimal.ONE, BigDecimal.ONE));
    }

    @Test
    @DisplayName("should keep the 5-argument constructor and of(): cache rates absent")
    void shouldKeepOldApiWithoutCacheRates() {
        var price = new ModelPrice("m", Set.of(), new BigDecimal("3.00"), new BigDecimal("15.00"), null);

        assertThat(price.cacheReadPerMtok()).isNull();
        assertThat(price.cacheWritePerMtok()).isNull();
        assertThat(ModelPrice.of("m", BigDecimal.ONE, BigDecimal.TEN).cacheReadPerMtok()).isNull();
        assertThat(price).isEqualTo(new ModelPrice("m", Set.of(), new BigDecimal("3.00"), new BigDecimal("15.00"),
                null, null, null));
    }

    @Test
    @DisplayName("should fall back to the input rate when a cache rate is absent, never to something cheaper")
    void shouldFallBackToInputRate() {
        var bare = ModelPrice.of("m", new BigDecimal("3.00"), new BigDecimal("15.00"));
        var full = bare.withCacheRates(new BigDecimal("0.30"), new BigDecimal("3.75"));
        var readOnly = bare.withCacheRates(new BigDecimal("0.30"), null);

        assertThat(bare.effectiveCacheReadPerMtok()).isEqualByComparingTo("3.00");
        assertThat(bare.effectiveCacheWritePerMtok()).isEqualByComparingTo("3.00");
        assertThat(full.effectiveCacheReadPerMtok()).isEqualByComparingTo("0.30");
        assertThat(full.effectiveCacheWritePerMtok()).isEqualByComparingTo("3.75");
        assertThat(readOnly.effectiveCacheReadPerMtok()).isEqualByComparingTo("0.30");
        assertThat(readOnly.effectiveCacheWritePerMtok()).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("should reject negative cache rates")
    void shouldRejectNegativeCacheRates() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ModelPrice("m", Set.of(), BigDecimal.ONE,
                BigDecimal.ONE, null, new BigDecimal("-0.1"), null)).withMessageContaining("cacheReadPerMtok");
        assertThatIllegalArgumentException().isThrownBy(() -> new ModelPrice("m", Set.of(), BigDecimal.ONE,
                BigDecimal.ONE, null, null, new BigDecimal("-0.1"))).withMessageContaining("cacheWritePerMtok");
    }
}
