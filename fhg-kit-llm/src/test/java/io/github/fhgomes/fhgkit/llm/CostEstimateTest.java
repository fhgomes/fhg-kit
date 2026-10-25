package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CostEstimateTest {

    @Test
    @DisplayName("should reject blank currency, negative amounts and null status")
    void shouldRejectInvalidValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CostEstimate(
                CostEstimate.Status.ESTIMATED, " ", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new CostEstimate(
                CostEstimate.Status.ESTIMATED, "USD", new BigDecimal("-0.1"), BigDecimal.ZERO, BigDecimal.ZERO, null));
        assertThatNullPointerException().isThrownBy(() -> new CostEstimate(
                null, "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null));
    }
}
