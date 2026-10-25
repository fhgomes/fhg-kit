package io.github.fhgomes.fhgkit.llm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * What the application knows about a model call before making it: why, for whom, and any extra dimensions.
 * {@link AiUsageTracker} copies these into the {@link AiUsageEvent}.
 *
 * @param purpose       what the application wants from the model, in its own vocabulary; never blank
 * @param tenantId      tenant to bill, when multi-tenant
 * @param userId        opaque id of the end user who triggered the call, when known
 * @param correlationId request, trace or run id
 * @param attributes    free-form dimensions (see {@link AiUsageEvent#attributes()}); never null, copied,
 *                      unmodifiable
 */
public record AiCallContext(
        String purpose,
        @Nullable String tenantId,
        @Nullable String userId,
        @Nullable String correlationId,
        Map<String, String> attributes) {

    /**
     * Validates the context and copies the attributes.
     *
     * @throws IllegalArgumentException when purpose is blank
     * @throws NullPointerException     when attributes, or an attribute key or value, is null
     */
    public AiCallContext {
        purpose = Checks.requireNonBlank(purpose, "purpose");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
    }

    /** A context with only a purpose. */
    public static AiCallContext of(String purpose) {
        return new AiCallContext(purpose, null, null, null, Map.of());
    }

    /** A builder; {@link Builder#purpose(String)} must be called. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled with the purpose. */
    public static Builder builder(String purpose) {
        return new Builder().purpose(purpose);
    }

    /** A copy of this context with one more attribute (or a replaced value). */
    public AiCallContext withAttribute(String key, String value) {
        var copy = new LinkedHashMap<>(attributes);
        copy.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        return new AiCallContext(purpose, tenantId, userId, correlationId, copy);
    }

    /** Fluent builder for {@link AiCallContext}. */
    public static final class Builder {

        private @Nullable String purpose;
        private @Nullable String tenantId;
        private @Nullable String userId;
        private @Nullable String correlationId;
        private final Map<String, String> attributes = new LinkedHashMap<>();

        private Builder() {
        }

        /** Required. */
        public Builder purpose(String purpose) {
            this.purpose = purpose;
            return this;
        }

        /** Optional. */
        public Builder tenantId(@Nullable String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /** Optional. */
        public Builder userId(@Nullable String userId) {
            this.userId = userId;
            return this;
        }

        /** Optional. */
        public Builder correlationId(@Nullable String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        /** Adds one attribute, replacing a previous value for the same key. */
        public Builder attribute(String key, String value) {
            attributes.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /** Adds every entry of the map. */
        public Builder attributes(Map<String, String> attributes) {
            Objects.requireNonNull(attributes, "attributes").forEach(this::attribute);
            return this;
        }

        /**
         * Creates the context.
         *
         * @throws NullPointerException     when purpose was not set
         * @throws IllegalArgumentException when purpose is blank
         */
        public AiCallContext build() {
            return new AiCallContext(
                    Objects.requireNonNull(purpose, "purpose"), tenantId, userId, correlationId, attributes);
        }
    }
}
