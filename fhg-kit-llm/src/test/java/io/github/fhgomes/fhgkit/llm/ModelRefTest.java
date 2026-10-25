package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ModelRefTest {

    @Test
    @DisplayName("should trim both parts and build the key as provider/modelId")
    void shouldTrimAndBuildKey() {
        var ref = new ModelRef("  openai ", " gpt-4o-mini\t");

        assertThat(ref.provider()).isEqualTo("openai");
        assertThat(ref.modelId()).isEqualTo("gpt-4o-mini");
        assertThat(ref.key()).isEqualTo("openai/gpt-4o-mini");
        assertThat(ref.isUnknown()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("should reject blank provider and blank model id")
    void shouldRejectBlankParts(String blank) {
        assertThatIllegalArgumentException().isThrownBy(() -> new ModelRef(blank, "m")).withMessageContaining("provider");
        assertThatIllegalArgumentException().isThrownBy(() -> new ModelRef("p", blank)).withMessageContaining("modelId");
    }

    @Test
    @DisplayName("should produce provider/unknown for unknown()")
    void shouldProduceUnknown() {
        var ref = ModelRef.unknown("anthropic");

        assertThat(ref.modelId()).isEqualTo(ModelRef.UNKNOWN_MODEL_ID);
        assertThat(ref.key()).isEqualTo("anthropic/unknown");
        assertThat(ref.isUnknown()).isTrue();
    }
}
