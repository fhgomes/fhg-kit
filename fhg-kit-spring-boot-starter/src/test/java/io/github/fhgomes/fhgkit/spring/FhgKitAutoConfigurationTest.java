package io.github.fhgomes.fhgkit.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FhgKitAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FhgKitAutoConfiguration.class));

    @Test
    void bindsPropertiesWithDefaults() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(FhgKitProperties.class);
            assertThat(ctx.getBean(FhgKitProperties.class).isEnabled()).isTrue();
        });
    }

    @Test
    void propertiesCanBeOverridden() {
        runner.withPropertyValues("fhg.kit.enabled=false")
            .run(ctx -> assertThat(ctx.getBean(FhgKitProperties.class).isEnabled()).isFalse());
    }
}
