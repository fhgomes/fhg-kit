# fhg-kit

fhg-kit — framework-free shared library for the fhgomes constellation of micro-SaaS products (LLM usage ledger and cost estimation first; credentials, flags, audit and integration contracts to follow)

Shared library (constellation **Kit**): versioned, framework-free core modules, no business logic, no secrets.
Applications pin one version in `gradle.properties` and upgrade deliberately.

## Modules

| Module | Purpose |
|---|---|
| `fhg-kit-bom` | Bill of materials — import it and omit versions on the other modules |
| `fhg-kit-llm` | TODO: one line on what this module offers |
| `fhg-kit-spring-boot-starter` | Spring Boot auto-configuration for the modules above (the only module that knows Spring) |

## Consume

```kotlin
// settings.gradle.kts or build.gradle.kts of the consuming app
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/fhgomes/fhg-kit")
        credentials {
            // GitHub Packages requires a token even for public packages (read:packages scope).
            username = System.getenv("GITHUB_ACTOR") ?: (findProperty("gpr.user") as String?)
            password = System.getenv("GITHUB_TOKEN") ?: (findProperty("gpr.key") as String?)
        }
    }
}

dependencies {
    implementation(platform("io.github.fhgomes:fhg-kit-bom:${property("fhgKitVersion")}"))
    implementation("io.github.fhgomes:fhg-kit-llm")
    implementation("io.github.fhgomes:fhg-kit-spring-boot-starter") // Spring Boot apps only
}
```

`gradle.properties` of the app: `fhgKitVersion=0.1.0`.

## Build

```bash
./gradlew build                 # compile + test every module
./gradlew publishToMavenLocal   # try a snapshot in a local app before releasing
```

## Release

1. Update `libVersion` in `gradle.properties` and move `[Unreleased]` entries in `CHANGELOG.md` under the new version.
2. Commit, then tag: `git tag v$(grep ^libVersion gradle.properties | cut -d= -f2) && git push --tags`.
3. The `publish` workflow builds, checks the tag against `libVersion` and publishes every module to GitHub Packages.

## Rules

- **No business logic.** A module enters the kit only after the same need exists in two applications.
- **Core modules are framework-free** (Java 21, `org.jspecify` only). Spring wiring lives in `fhg-kit-spring-boot-starter`.
- **Ports, not implementations.** Anything an app must plug in is an interface here and an adapter there.
- **SemVer.** Breaking a public type means a major bump and a `CHANGELOG` entry with a migration note.
- **Open-source hygiene.** No credentials, hostnames, internal URLs or personal data in code, tests, or docs. Everything configurable comes from the consumer. Run a secrets/data-exposure review before every release.

## License

Apache License 2.0 — see `LICENSE`.
