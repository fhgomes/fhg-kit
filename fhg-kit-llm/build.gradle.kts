plugins {
    `java-library`
}

description = "fhg-kit — LLM usage accounting: usage events, recorders, price table, cost estimation and call tracker"

dependencies {
    // Keep this module free of frameworks. Ports (interfaces) here, adapters in the app or in the starter.
    // Only org.jspecify (api) and org.slf4j (implementation) are inherited from the root build.
    testRuntimeOnly("org.slf4j:slf4j-simple:${property("slf4jVersion")}")
}
