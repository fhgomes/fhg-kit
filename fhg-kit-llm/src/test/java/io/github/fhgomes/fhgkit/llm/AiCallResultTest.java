package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiCallResultTest {

    @Test
    @DisplayName("of should carry value, model and tokens")
    void ofShouldCarryParts() {
        var model = new ModelRef("p", "m");
        var result = AiCallResult.of("answer", model, TokenUsage.exact(1, 2));

        assertThat(result.value()).isEqualTo("answer");
        assertThat(result.model()).isEqualTo(model);
        assertThat(result.tokens()).isEqualTo(TokenUsage.exact(1, 2));
    }

    @Test
    @DisplayName("of should reject nulls")
    void ofShouldRejectNulls() {
        var model = new ModelRef("p", "m");

        assertThatNullPointerException().isThrownBy(() -> AiCallResult.of(null, model, TokenUsage.zero()));
        assertThatNullPointerException().isThrownBy(() -> AiCallResult.of("v", null, TokenUsage.zero()));
        assertThatNullPointerException().isThrownBy(() -> AiCallResult.of("v", model, null));
    }
}
