package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiCallContextTest {

    @Test
    @DisplayName("of(purpose) should create a context with nothing else")
    void ofShouldCreateMinimalContext() {
        var ctx = AiCallContext.of(" summarize ");

        assertThat(ctx.purpose()).isEqualTo("summarize");
        assertThat(ctx.tenantId()).isNull();
        assertThat(ctx.userId()).isNull();
        assertThat(ctx.correlationId()).isNull();
        assertThat(ctx.attributes()).isEmpty();
    }

    @Test
    @DisplayName("builder should carry every field and copy attributes")
    void builderShouldCarryEveryField() {
        var ctx = AiCallContext.builder("extract")
                .tenantId("t").userId("u").correlationId("c")
                .attribute("operation", "extract")
                .attributes(Map.of("promptVersion", "v3"))
                .build();

        assertThat(ctx.tenantId()).isEqualTo("t");
        assertThat(ctx.userId()).isEqualTo("u");
        assertThat(ctx.correlationId()).isEqualTo("c");
        assertThat(ctx.attributes()).containsOnly(Map.entry("operation", "extract"), Map.entry("promptVersion", "v3"));
        assertThatThrownBy(() -> ctx.attributes().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("withAttribute should return a copy and leave the original untouched")
    void withAttributeShouldCopy() {
        var original = AiCallContext.of("p");

        var copy = original.withAttribute("turnIndex", "2");

        assertThat(original.attributes()).isEmpty();
        assertThat(copy.attributes()).containsEntry("turnIndex", "2");
        assertThat(copy.purpose()).isEqualTo("p");
    }

    @Test
    @DisplayName("should reject blank purpose and null attributes")
    void shouldRejectInvalidValues() {
        var mutable = new HashMap<String, String>();
        mutable.put("k", null);

        assertThatIllegalArgumentException().isThrownBy(() -> AiCallContext.of(""));
        assertThatNullPointerException().isThrownBy(() -> AiCallContext.builder().build()).withMessage("purpose");
        assertThatNullPointerException().isThrownBy(() -> new AiCallContext("p", null, null, null, null));
        assertThatNullPointerException().isThrownBy(() -> new AiCallContext("p", null, null, null, mutable));
    }
}
