package io.github.fhgomes.fhgkit.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point of the starter. Register one {@code @Bean} per kit port here, always
 * {@code @ConditionalOnMissingBean} so applications can override any piece.
 */
@AutoConfiguration
@EnableConfigurationProperties(FhgKitProperties.class)
public class FhgKitAutoConfiguration {
}
