package io.github.fhgomes.fhgkit.spring;

import io.github.fhgomes.fhgkit.llm.AiUsageRecorder;
import io.github.fhgomes.fhgkit.llm.AiUsageRecorders;
import io.github.fhgomes.fhgkit.llm.AiUsageTracker;
import io.github.fhgomes.fhgkit.llm.CostEstimator;
import io.github.fhgomes.fhgkit.llm.DefaultCostEstimator;
import io.github.fhgomes.fhgkit.llm.InMemoryPriceTable;
import io.github.fhgomes.fhgkit.llm.ModelPrice;
import io.github.fhgomes.fhgkit.llm.PriceTable;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Entry point of the starter. Register one {@code @Bean} per kit port here, always
 * {@code @ConditionalOnMissingBean} so applications can override any piece.
 *
 * <p>Property binding is unconditional; each module's beans sit in a nested configuration guarded by that
 * module's {@code enabled} flag (and the master {@code fhg.kit.enabled}).
 */
@AutoConfiguration
@EnableConfigurationProperties(FhgKitProperties.class)
public class FhgKitAutoConfiguration {

    /** Default {@link PriceTable#source()} when {@code fhg.kit.llm.pricing.source} is not set. */
    static final String DEFAULT_PRICING_SOURCE = "fhg.kit.llm.pricing";

    /**
     * Beans of {@code fhg-kit-llm}: a {@link PriceTable} bound from {@code fhg.kit.llm.pricing}, a
     * {@link CostEstimator} on top of it, a logging {@link AiUsageRecorder} and an {@link AiUsageTracker} tying
     * them together. Define any of these beans in the application to replace the default.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "fhg.kit", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "fhg.kit.llm", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class LlmConfiguration {

        /** In-memory price table built from {@code fhg.kit.llm.pricing}; empty when no model is configured. */
        @Bean
        @ConditionalOnMissingBean
        public PriceTable fhgKitPriceTable(FhgKitProperties properties) {
            return priceTable(properties.getLlm().getPricing());
        }

        /** {@link DefaultCostEstimator} over the {@link PriceTable} bean. */
        @Bean
        @ConditionalOnMissingBean
        public CostEstimator fhgKitCostEstimator(PriceTable priceTable) {
            return new DefaultCostEstimator(priceTable);
        }

        /** {@link AiUsageRecorders#logging()}: one INFO line per call until the application plugs in a ledger. */
        @Bean
        @ConditionalOnMissingBean
        public AiUsageRecorder fhgKitAiUsageRecorder() {
            return AiUsageRecorders.logging();
        }

        /** Tracker wired to the recorder and estimator beans, on the system UTC clock. */
        @Bean
        @ConditionalOnMissingBean
        public AiUsageTracker fhgKitAiUsageTracker(AiUsageRecorder recorder, CostEstimator estimator) {
            return new AiUsageTracker(recorder, estimator, Clock.systemUTC());
        }

        static InMemoryPriceTable priceTable(FhgKitProperties.Pricing pricing) {
            List<ModelPrice> prices = new ArrayList<>();
            for (FhgKitProperties.ModelEntry entry : pricing.getModels()) {
                prices.add(new ModelPrice(
                        requireId(entry),
                        new LinkedHashSet<>(entry.getAliases()),
                        entry.getInputPerMtok(),
                        entry.getOutputPerMtok(),
                        entry.getContextWindow()));
            }
            String source = pricing.getSource() != null ? pricing.getSource() : DEFAULT_PRICING_SOURCE;
            LocalDate asOf = pricing.getAsOf() != null ? pricing.getAsOf() : LocalDate.now(Clock.systemUTC());
            return new InMemoryPriceTable(prices, pricing.getCurrency(), source, asOf);
        }

        private static String requireId(FhgKitProperties.ModelEntry entry) {
            String id = entry.getId();
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("fhg.kit.llm.pricing.models[*].id is required for every entry");
            }
            return id;
        }
    }
}
