package io.github.fhgomes.fhgkit.llm;

/**
 * How a model call ended, from the accounting point of view.
 *
 * <p>Only {@link #OK} normally carries token counts; every failure is recorded with zero tokens unless the
 * consumer knows better.
 */
public enum AiCallOutcome {

    /** The call completed and its output was accepted by the application. */
    OK,

    /** The call did not complete within the deadline (connect, read or overall timeout). */
    FAILED_TIMEOUT,

    /**
     * The provider or the transport failed: HTTP 5xx, rate limiting, authentication, network error, or any
     * exception the application did not classify more precisely.
     */
    FAILED_UPSTREAM,

    /**
     * The model answered, but the application could not use the answer: malformed JSON, schema violation,
     * missing fields, failed validation. Tokens were usually consumed.
     */
    INVALID_OUTPUT,

    /** The model declined to answer (safety filter, policy refusal, content blocked by the provider). */
    REFUSED
}
