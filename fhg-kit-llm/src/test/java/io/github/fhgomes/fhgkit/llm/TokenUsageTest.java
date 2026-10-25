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
}
