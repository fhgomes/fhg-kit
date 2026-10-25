package io.github.fhgomes.fhgkit.llm;

/**
 * Thrown by an application when the model declined to answer: safety filter, policy refusal, content blocked by
 * the provider.
 *
 * <p>{@link AiUsageTracker} records it as {@link AiCallOutcome#REFUSED}. Only the class name reaches the usage
 * event.
 */
public class ModelRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param message why the model refused, as reported by the provider */
    public ModelRefusedException(String message) {
        super(message);
    }

    /**
     * @param message why the model refused
     * @param cause   the provider exception, when there is one
     */
    public ModelRefusedException(String message, Throwable cause) {
        super(message, cause);
    }
}
