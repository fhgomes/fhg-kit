package io.github.fhgomes.fhgkit.llm;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Port: where model prices come from.
 *
 * <p>The kit ships {@link InMemoryPriceTable}; an application may implement this interface over a database or a
 * remote catalogue. Implementations must be thread-safe.
 */
public interface PriceTable {

    /**
     * Finds the price of a model.
     *
     * @param model the model to price; implementations are expected to match on {@link ModelRef#modelId()}
     * @return the price, or empty when the model is not in the table
     */
    Optional<ModelPrice> lookup(ModelRef model);

    /** ISO 4217 code every price in this table is expressed in. */
    String currency();

    /** Where the prices were taken from (a label for humans: a URL, a document name, "manual"). */
    String source();

    /** Date the prices were valid at. Prices change; estimates should say which list they used. */
    LocalDate asOf();
}
