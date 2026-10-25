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
}
