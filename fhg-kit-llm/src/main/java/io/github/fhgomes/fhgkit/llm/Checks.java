package io.github.fhgomes.fhgkit.llm;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Argument validation shared by the records of this package. Package-private on purpose. */
final class Checks {

    private Checks() {
    }

    /** Returns the trimmed value, or throws when it is {@code null} or blank. */
    static String requireNonBlank(@Nullable String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative, got " + value);
        }
        return value;
    }

    static BigDecimal requireNonNegative(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative, got " + value);
        }
        return value;
    }
}
