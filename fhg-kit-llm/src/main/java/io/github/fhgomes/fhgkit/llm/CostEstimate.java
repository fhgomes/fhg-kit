package io.github.fhgomes.fhgkit.llm;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Money attributed to one model call, as computed by a {@link CostEstimator}.
 *
 * <p>Amounts are never negative and are expressed in {@link #currency()}. When the status is not
 * {@link Status#ESTIMATED} all amounts are zero and the consumer should treat the cost as unknown rather than
 * free.
 *
 * @param status   whether the amounts are meaningful
 * @param currency ISO 4217 code of the amounts (as declared by the price table), never blank
 * @param input    cost of the input tokens
 * @param output   cost of the output tokens
 * @param total    {@code input + output}
 * @param pricedAs id of the {@link ModelPrice} that was applied, or {@code null} when no price matched
 */
public record CostEstimate(
        Status status,
        String currency,
        BigDecimal input,
        BigDecimal output,
        BigDecimal total,
        @Nullable String pricedAs) {

    /** Why the amounts are what they are. */
    public enum Status {
        /** A price was found and the tokens were known: the amounts are an estimate at list price. */
        ESTIMATED,
        /** No entry in the price table matched the model: amounts are zero and meaningless. */
        UNKNOWN_MODEL,
        /**
         * The model is priced but the token counts were estimated as zero, i.e. not known at all: amounts are
         * zero and meaningless. Distinguishes "we do not know" from "it was free".
         */
        UNKNOWN_TOKENS
    }

    /**
     * Validates the estimate.
     *
     * @throws IllegalArgumentException when the currency is blank or an amount is negative
     * @throws NullPointerException     when a required part is null
     */
    public CostEstimate {
        Objects.requireNonNull(status, "status");
        currency = Checks.requireNonBlank(currency, "currency");
        Checks.requireNonNegative(input, "input");
        Checks.requireNonNegative(output, "output");
        Checks.requireNonNegative(total, "total");
    }

    /** Returns {@code true} when {@link #status()} is {@link Status#ESTIMATED}. */
    public boolean isKnown() {
        return status == Status.ESTIMATED;
    }
}
