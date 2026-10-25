package io.github.fhgomes.fhgkit.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code fhg.kit.*} — every value must have a safe default or be explicitly required by the consumer.
 * Never a secret with a default.
 */
@ConfigurationProperties(prefix = "fhg.kit")
public class FhgKitProperties {

    /** Master switch for the auto-configuration. */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
