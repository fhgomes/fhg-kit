package io.github.fhgomes.fhgkit.spring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code fhg.kit.*} — every value must have a safe default or be explicitly required by the consumer.
 * Never a secret with a default.
 *
 * <pre>
 * fhg:
 *   kit:
 *     llm:
 *       enabled: true
 *       pricing:
 *         currency: USD
 *         source: "provider price list"
 *         as-of: 2026-01-01
 *         models:
 *           - id: model-a
 *             aliases: [model-a-2026-01-01]
 *             input-per-mtok: 3.00
 *             output-per-mtok: 15.00
 *             context-window: 128000
 * </pre>
 */
@ConfigurationProperties(prefix = "fhg.kit")
public class FhgKitProperties {

    /** Master switch for the auto-configuration. */
    private boolean enabled = true;

    /** LLM usage accounting (module {@code fhg-kit-llm}). */
    private final Llm llm = new Llm();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Llm getLlm() {
        return llm;
    }

    /** {@code fhg.kit.llm.*}. */
    public static class Llm {

        /** Whether to register the LLM accounting beans (price table, cost estimator, recorder, tracker). */
        private boolean enabled = true;

        /** Model prices used by the default cost estimator. */
        private final Pricing pricing = new Pricing();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Pricing getPricing() {
            return pricing;
        }
    }

    /** {@code fhg.kit.llm.pricing.*} — the price table, maintained by the operator of the application. */
    public static class Pricing {

        /** ISO 4217 currency of every price in the table. */
        private String currency = "USD";

        /** Where the prices were taken from (a label for humans). Defaults to "fhg.kit.llm.pricing". */
        private @Nullable String source;

        /** Date the prices were valid at. Defaults to the day the application started. */
        private @Nullable LocalDate asOf;

        /** One entry per priced model. An empty list is allowed: every estimate is then UNKNOWN_MODEL. */
        private List<ModelEntry> models = new ArrayList<>();

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public @Nullable String getSource() {
            return source;
        }

        public void setSource(@Nullable String source) {
            this.source = source;
        }

        public @Nullable LocalDate getAsOf() {
            return asOf;
        }

        public void setAsOf(@Nullable LocalDate asOf) {
            this.asOf = asOf;
        }

        public List<ModelEntry> getModels() {
            return models;
        }

        public void setModels(List<ModelEntry> models) {
            this.models = models;
        }
    }

    /** One priced model: {@code fhg.kit.llm.pricing.models[n].*}. */
    public static class ModelEntry {

        /** Canonical model id, as the provider names it. Required. */
        private @Nullable String id;

        /** Other ids that resolve to this price (dated snapshots, gateway names). */
        private List<String> aliases = new ArrayList<>();

        /** Price of one million input tokens. Required. */
        private @Nullable BigDecimal inputPerMtok;

        /** Price of one million output tokens. Required. */
        private @Nullable BigDecimal outputPerMtok;

        /** Maximum context size in tokens, when known. */
        private @Nullable Integer contextWindow;

        public @Nullable String getId() {
            return id;
        }

        public void setId(@Nullable String id) {
            this.id = id;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public void setAliases(List<String> aliases) {
            this.aliases = aliases;
        }

        public @Nullable BigDecimal getInputPerMtok() {
            return inputPerMtok;
        }

        public void setInputPerMtok(@Nullable BigDecimal inputPerMtok) {
            this.inputPerMtok = inputPerMtok;
        }

        public @Nullable BigDecimal getOutputPerMtok() {
            return outputPerMtok;
        }

        public void setOutputPerMtok(@Nullable BigDecimal outputPerMtok) {
            this.outputPerMtok = outputPerMtok;
        }

        public @Nullable Integer getContextWindow() {
            return contextWindow;
        }

        public void setContextWindow(@Nullable Integer contextWindow) {
            this.contextWindow = contextWindow;
        }
    }
}
