package io.github.fhgomes.fhgkit.llm;

import java.util.Objects;

/**
 * What a model call returns to {@link AiUsageTracker}: the application's value plus the accounting facts only
 * the call site knows (which model actually answered, how many tokens it used).
 *
 * @param <T> the application's result type
 */
public interface AiCallResult<T> {

    /** The application's result, handed back by {@link AiUsageTracker#track}. */
    T value();

    /** The model that actually served the call (a gateway may have routed to another than requested). */
    ModelRef model();

    /** Tokens consumed, as reported by the provider or estimated by the application. */
    TokenUsage tokens();

    /**
     * Creates a result.
     *
     * @param value  the application's result
     * @param model  the model that served the call
     * @param tokens the tokens it consumed
     * @param <T>    the application's result type
     * @return an immutable result
     */
    static <T> AiCallResult<T> of(T value, ModelRef model, TokenUsage tokens) {
        return new Simple<>(value, model, tokens);
    }

    /** Default record implementation. */
    record Simple<T>(T value, ModelRef model, TokenUsage tokens) implements AiCallResult<T> {

        /** Validates that nothing is null. */
        public Simple {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(tokens, "tokens");
        }
    }
}
