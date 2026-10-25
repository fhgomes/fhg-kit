package io.github.fhgomes.fhgkit.llm;

/**
 * Identifies a model by provider and model id, e.g. {@code openai} / {@code gpt-4o-mini}.
 *
 * <p>Both parts are trimmed and must not be blank. {@link #key()} is the stable, provider-qualified string
 * used in logs and ledgers.
 *
 * @param provider name of the provider or gateway the call went through (consumer-defined vocabulary)
 * @param modelId  the provider's identifier for the model; {@value #UNKNOWN_MODEL_ID} when it is not known
 */
public record ModelRef(String provider, String modelId) {

    /** Model id used by {@link #unknown(String)} when a call fails before the model is known. */
    public static final String UNKNOWN_MODEL_ID = "unknown";

    /**
     * Validates and normalizes both parts.
     *
     * @throws IllegalArgumentException when either part is null or blank
     */
    public ModelRef {
        provider = Checks.requireNonBlank(provider, "provider");
        modelId = Checks.requireNonBlank(modelId, "modelId");
    }

    /** Returns {@code provider + "/" + modelId}. */
    public String key() {
        return provider + "/" + modelId;
    }

    /**
     * Returns a reference whose model id is {@value #UNKNOWN_MODEL_ID}, for events emitted when the call failed
     * before a model was resolved (for example a routing or authentication error).
     *
     * @param provider the provider that was targeted
     * @return a reference to {@code provider/unknown}
     */
    public static ModelRef unknown(String provider) {
        return new ModelRef(provider, UNKNOWN_MODEL_ID);
    }

    /** Returns {@code true} when this reference was produced by {@link #unknown(String)}. */
    public boolean isUnknown() {
        return UNKNOWN_MODEL_ID.equals(modelId);
    }
}
