package io.github.fhgomes.fhgkit.llm;

/**
 * Thrown by an application when the model answered but the answer cannot be used: malformed JSON, schema
 * violation, missing fields, failed business validation.
 *
 * <p>{@link AiUsageTracker} records it as {@link AiCallOutcome#INVALID_OUTPUT}. Only the class name reaches the
 * usage event, so the message may be as detailed as the application's own error handling needs.
 */
public class InvalidModelOutputException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param message what was wrong with the output */
    public InvalidModelOutputException(String message) {
        super(message);
    }

    /**
     * @param message what was wrong with the output
     * @param cause   the parsing or validation failure
     */
    public InvalidModelOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
