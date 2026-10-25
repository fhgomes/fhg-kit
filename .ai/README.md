# fhg-kit — AI Context

> fhg-kit — framework-free shared library for the fhgomes constellation of micro-SaaS products (LLM usage ledger and cost estimation first; credentials, flags, audit and integration contracts to follow)

## Project Type
Java shared library (Gradle multi-module, published to GitHub Packages)

## Tech Stack
Java 21, Gradle 8.14.3 (Kotlin DSL), JUnit 6, AssertJ, jspecify, Spring Boot 4.1.2 (starter module only), GitHub Actions

## Key Directories
- `fhg-kit-llm/` -- framework-free module (ports + records)
- `fhg-kit-spring-boot-starter/` -- the only module that depends on Spring
- `.ai/` -- AI agent context and guidelines

## Quick Reference
- **Build:** ./gradlew build
- **Test:** ./gradlew test
- **Run:** ./gradlew publishToMavenLocal   # library: no run target

## Related Docs
- [Architecture](architecture.md)
- [Guidelines](guidelines.md)
- [Testing](testing-framework.md)
