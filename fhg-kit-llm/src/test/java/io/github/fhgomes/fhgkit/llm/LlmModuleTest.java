package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LlmModuleTest {

    @Test
    void packageIsNullMarked() {
        var pkg = getClass().getPackage();
        assertThat(pkg.isAnnotationPresent(org.jspecify.annotations.NullMarked.class))
            .as("package-info.java must declare @NullMarked")
            .isTrue();
    }
}
