package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryPriceTableTest {

    private static final ModelPrice MODEL_A = new ModelPrice(
            "Model-A", Set.of("model-a-2026-01-01", "gateway/model-a"), new BigDecimal("3.00"), new BigDecimal("15.00"), 128_000);
    private static final ModelPrice MODEL_B = ModelPrice.of("model-b", new BigDecimal("0.15"), new BigDecimal("0.60"));

    private final InMemoryPriceTable table = new InMemoryPriceTable(
            List.of(MODEL_A, MODEL_B), "USD", "unit-test", LocalDate.of(2026, 1, 1));

    @Test
    @DisplayName("should match the id exactly, ignoring case and surrounding whitespace")
    void shouldMatchIdExactly() {
        assertThat(table.lookup(new ModelRef("p", "model-a"))).contains(MODEL_A);
        assertThat(table.lookup(new ModelRef("p", "MODEL-A"))).contains(MODEL_A);
        assertThat(table.lookup(new ModelRef("p", "  model-b "))).contains(MODEL_B);
    }

    @Test
    @DisplayName("should match aliases and report the canonical entry")
    void shouldMatchAliases() {
        assertThat(table.lookup(new ModelRef("p", "model-a-2026-01-01"))).contains(MODEL_A);
        assertThat(table.lookup(new ModelRef("p", "GATEWAY/model-a"))).contains(MODEL_A);
    }

    @Test
    @DisplayName("should never match by prefix")
    void shouldNotMatchByPrefix() {
        assertThat(table.lookup(new ModelRef("p", "model-a-2027-01-01"))).isEmpty();
        assertThat(table.lookup(new ModelRef("p", "model"))).isEmpty();
        assertThat(table.lookup(ModelRef.unknown("p"))).isEmpty();
    }

    @Test
    @DisplayName("should expose currency, source, asOf and entries")
    void shouldExposeMetadata() {
        assertThat(table.currency()).isEqualTo("USD");
        assertThat(table.source()).isEqualTo("unit-test");
        assertThat(table.asOf()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(table.prices()).containsExactly(MODEL_A, MODEL_B);
        assertThat(table.size()).isEqualTo(2);
        assertThat(InMemoryPriceTable.empty("EUR").size()).isZero();
    }

    @Test
    @DisplayName("should reject an id or alias declared by two entries")
    void shouldRejectDuplicates() {
        var sameIdDifferentCase = ModelPrice.of("MODEL-B", BigDecimal.ONE, BigDecimal.ONE);
        var aliasCollidesWithId = new ModelPrice("other", Set.of("model-b"), BigDecimal.ONE, BigDecimal.ONE, null);
        var aliasCollidesWithAlias = new ModelPrice("other", Set.of("gateway/model-a"), BigDecimal.ONE, BigDecimal.ONE, null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InMemoryPriceTable(List.of(MODEL_A, MODEL_B, sameIdDifferentCase), "USD", "s", LocalDate.EPOCH))
                .withMessageContaining("MODEL-B");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InMemoryPriceTable(List.of(MODEL_B, aliasCollidesWithId), "USD", "s", LocalDate.EPOCH))
                .withMessageContaining("model-b");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InMemoryPriceTable(List.of(MODEL_A, aliasCollidesWithAlias), "USD", "s", LocalDate.EPOCH))
                .withMessageContaining("gateway/model-a");
    }

    @Test
    @DisplayName("should reject blank currency or source")
    void shouldRejectBlankMetadata() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InMemoryPriceTable(List.of(), " ", "s", LocalDate.EPOCH))
                .withMessageContaining("currency");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InMemoryPriceTable(List.of(), "USD", "", LocalDate.EPOCH))
                .withMessageContaining("source");
    }
}
