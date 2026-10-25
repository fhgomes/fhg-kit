package io.github.fhgomes.fhgkit.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.fhgomes.fhgkit.llm.AiUsageRecorder;
import io.github.fhgomes.fhgkit.llm.AiUsageRecorders;
import io.github.fhgomes.fhgkit.llm.AiUsageTracker;
import io.github.fhgomes.fhgkit.llm.CostEstimate;
import io.github.fhgomes.fhgkit.llm.CostEstimator;
import io.github.fhgomes.fhgkit.llm.DefaultCostEstimator;
import io.github.fhgomes.fhgkit.llm.InMemoryPriceTable;
import io.github.fhgomes.fhgkit.llm.ModelRef;
import io.github.fhgomes.fhgkit.llm.PriceTable;
import io.github.fhgomes.fhgkit.llm.TokenUsage;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class FhgKitAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FhgKitAutoConfiguration.class));

    @Test
    void bindsPropertiesWithDefaults() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(FhgKitProperties.class);
            var properties = ctx.getBean(FhgKitProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getLlm().isEnabled()).isTrue();
            assertThat(properties.getLlm().getPricing().getCurrency()).isEqualTo("USD");
            assertThat(properties.getLlm().getPricing().getModels()).isEmpty();
        });
    }

    @Test
    void propertiesCanBeOverridden() {
        runner.withPropertyValues("fhg.kit.enabled=false")
            .run(ctx -> assertThat(ctx.getBean(FhgKitProperties.class).isEnabled()).isFalse());
    }

    @Test
    @DisplayName("defaults should register price table, cost estimator, logging recorder and tracker")
    void defaultsShouldRegisterLlmBeans() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(PriceTable.class);
            assertThat(ctx).hasSingleBean(CostEstimator.class);
            assertThat(ctx).hasSingleBean(AiUsageRecorder.class);
            assertThat(ctx).hasSingleBean(AiUsageTracker.class);

            var table = ctx.getBean(PriceTable.class);
            assertThat(table).isInstanceOf(InMemoryPriceTable.class);
            assertThat(((InMemoryPriceTable) table).size()).isZero();
            assertThat(table.currency()).isEqualTo("USD");
            assertThat(table.source()).isEqualTo(FhgKitAutoConfiguration.DEFAULT_PRICING_SOURCE);
            assertThat(ctx.getBean(CostEstimator.class)).isInstanceOf(DefaultCostEstimator.class);
            assertThat(ctx.getBean(AiUsageRecorder.class)).isSameAs(AiUsageRecorders.logging());
            assertThat(ctx.getBean(CostEstimator.class).estimate(new ModelRef("p", "m"), TokenUsage.exact(1, 1)).status())
                .isEqualTo(CostEstimate.Status.UNKNOWN_MODEL);
        });
    }

    @Test
    @DisplayName("fhg.kit.llm.enabled=false should register none of the LLM beans")
    void disabledLlmShouldRegisterNothing() {
        runner.withPropertyValues("fhg.kit.llm.enabled=false").run(ctx -> {
            assertThat(ctx).hasSingleBean(FhgKitProperties.class);
            assertThat(ctx).doesNotHaveBean(PriceTable.class);
            assertThat(ctx).doesNotHaveBean(CostEstimator.class);
            assertThat(ctx).doesNotHaveBean(AiUsageRecorder.class);
            assertThat(ctx).doesNotHaveBean(AiUsageTracker.class);
        });
    }

    @Test
    @DisplayName("fhg.kit.enabled=false (master switch) should register none of the LLM beans")
    void disabledMasterSwitchShouldRegisterNothing() {
        runner.withPropertyValues("fhg.kit.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(PriceTable.class);
            assertThat(ctx).doesNotHaveBean(AiUsageTracker.class);
        });
    }

    @Test
    @DisplayName("pricing properties should bind into a working price table, matched by alias")
    void pricingPropertiesShouldBind() {
        runner.withPropertyValues(
                "fhg.kit.llm.pricing.currency=EUR",
                "fhg.kit.llm.pricing.source=unit test",
                "fhg.kit.llm.pricing.as-of=2026-01-15",
                "fhg.kit.llm.pricing.models[0].id=model-a",
                "fhg.kit.llm.pricing.models[0].aliases[0]=model-a-2026-01-01",
                "fhg.kit.llm.pricing.models[0].aliases[1]=gateway/model-a",
                "fhg.kit.llm.pricing.models[0].input-per-mtok=3.00",
                "fhg.kit.llm.pricing.models[0].output-per-mtok=15.00",
                "fhg.kit.llm.pricing.models[0].context-window=128000",
                "fhg.kit.llm.pricing.models[1].id=model-b",
                "fhg.kit.llm.pricing.models[1].input-per-mtok=0.10",
                "fhg.kit.llm.pricing.models[1].output-per-mtok=0.40")
            .run(ctx -> {
                var table = ctx.getBean(PriceTable.class);
                assertThat(table.currency()).isEqualTo("EUR");
                assertThat(table.source()).isEqualTo("unit test");
                assertThat(table.asOf()).isEqualTo(LocalDate.of(2026, 1, 15));

                var byAlias = table.lookup(new ModelRef("p", "Gateway/Model-A"));
                assertThat(byAlias).isPresent();
                assertThat(byAlias.get().id()).isEqualTo("model-a");
                assertThat(byAlias.get().aliases()).containsExactlyInAnyOrder("model-a-2026-01-01", "gateway/model-a");
                assertThat(byAlias.get().contextWindow()).isEqualTo(128_000);
                assertThat(table.lookup(new ModelRef("p", "model-b"))).isPresent();
                assertThat(table.lookup(new ModelRef("p", "model-c"))).isEmpty();

                var cost = ctx.getBean(CostEstimator.class)
                    .estimate(new ModelRef("p", "model-a-2026-01-01"), TokenUsage.exact(1_000_000, 0));
                assertThat(cost.status()).isEqualTo(CostEstimate.Status.ESTIMATED);
                assertThat(cost.currency()).isEqualTo("EUR");
                assertThat(cost.total()).isEqualTo(new BigDecimal("3.000000"));
                assertThat(cost.pricedAs()).isEqualTo("model-a");
            });
    }

    @Test
    @DisplayName("a model entry missing one side of the price should fail the context with a clear message")
    void oneSidedPriceShouldFailStartup() {
        runner.withPropertyValues(
                "fhg.kit.llm.pricing.models[0].id=model-a",
                "fhg.kit.llm.pricing.models[0].input-per-mtok=3.00")
            .run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx).getFailure().rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("model-a")
                    .hasMessageContaining("outputPerMtok");
            });
    }

    @Test
    @DisplayName("a user-defined AiUsageRecorder bean should win over the default and feed the tracker")
    void userRecorderShouldWin() {
        runner.withUserConfiguration(CustomRecorderConfiguration.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(AiUsageRecorder.class);
            assertThat(ctx.getBean(AiUsageRecorder.class)).isSameAs(CustomRecorderConfiguration.RECORDER);
            assertThat(ctx).hasSingleBean(AiUsageTracker.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRecorderConfiguration {

        static final AiUsageRecorder RECORDER = AiUsageRecorders.noop();

        @Bean
        AiUsageRecorder myRecorder() {
            return RECORDER;
        }
    }
}
