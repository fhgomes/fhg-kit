package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenUsageTest {

    @Test
    @DisplayName("should sum input and output in total()")
    void shouldSumTotal() {
        assertThat(TokenUsage.exact(120, 30).total()).isEqualTo(150);
    }

    @Test
    @DisplayName("should reject negative counts")
    void shouldRejectNegativeCounts() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenUsage(-1, 0, false)).withMessageContaining("input");
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenUsage(0, -1, false)).withMessageContaining("output");
    }

    @Test
    @DisplayName("should distinguish zero, exact and estimated factories")
    void shouldDistinguishFactories() {
        assertThat(TokenUsage.zero()).isEqualTo(new TokenUsage(0, 0, false));
        assertThat(TokenUsage.exact(1, 2)).isEqualTo(new TokenUsage(1, 2, false));
        assertThat(TokenUsage.estimated(1, 2)).isEqualTo(new TokenUsage(1, 2, true));
        assertThat(TokenUsage.estimated(1, 2).estimated()).isTrue();
    }

    @Test
    @DisplayName("should keep the 3-argument constructor and the factories working with zero cache parts")
    void shouldKeepOldApiWithZeroCache() {
        var old = new TokenUsage(10, 5, true);

        assertThat(old.cacheRead()).isZero();
        assertThat(old.cacheWrite()).isZero();
        assertThat(old).isEqualTo(new TokenUsage(10, 5, true, 0, 0));
        assertThat(TokenUsage.exact(10, 5).cacheRead()).isZero();
        assertThat(TokenUsage.estimated(10, 5).cacheWrite()).isZero();
        assertThat(TokenUsage.zero().cacheRead()).isZero();
    }

    @Test
    @DisplayName("should carry cache parts as parts of input: total() stays input + output")
    void shouldCarryCacheAsPartsOfInput() {
        var usage = TokenUsage.exact(36_000, 100, 35_000, 900);

        assertThat(usage.input()).isEqualTo(36_000);
        assertThat(usage.cacheRead()).isEqualTo(35_000);
        assertThat(usage.cacheWrite()).isEqualTo(900);
        assertThat(usage.estimated()).isFalse();
        assertThat(usage.total()).isEqualTo(36_100);
        assertThat(usage.uncachedInput()).isEqualTo(100);
    }

    @Test
    @DisplayName("should reject negative cache parts and cacheRead + cacheWrite above input")
    void shouldRejectInvalidCacheParts() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenUsage(10, 0, false, -1, 0))
                .withMessageContaining("cacheRead");
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenUsage(10, 0, false, 0, -1))
                .withMessageContaining("cacheWrite");
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenUsage(10, 0, false, 6, 5))
                .withMessageContaining("cacheRead + cacheWrite");
        assertThatIllegalArgumentException().isThrownBy(
                () -> new TokenUsage(Integer.MAX_VALUE, 0, false, Integer.MAX_VALUE, 1));
        assertThat(new TokenUsage(10, 0, false, 6, 4).uncachedInput()).isZero();
    }
}
