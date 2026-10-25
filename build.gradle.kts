// fhg-kit — shared library (constellation Kit).
// Rules: no business logic, no secrets, no Spring in core modules, every public type documented, semver.

plugins {
    base
}

val libVersion: String by project

allprojects {
    group = "io.github.fhgomes"
    version = libVersion
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "maven-publish")

    // Library modules (everything except the BOM)
    plugins.withId("java-library") {
        configure<JavaPluginExtension> {
            toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
            withSourcesJar()
            withJavadocJar()
        }
        dependencies {
            "api"("org.jspecify:jspecify:${property("jspecifyVersion")}")
            "implementation"("org.slf4j:slf4j-api:${property("slf4jVersion")}")
            "testImplementation"(platform("org.junit:junit-bom:${property("junitVersion")}"))
            "testImplementation"("org.junit.jupiter:junit-jupiter")
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
            "testImplementation"("org.assertj:assertj-core:${property("assertjVersion")}")
        }
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-serial,-processing"))
        }
        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
            isFailOnError = false // a module that only has package-info.java yet must still build
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
        }
        configure<PublishingExtension> {
            publications.create<MavenPublication>("maven") { from(components["java"]) }
        }
    }

    // BOM module
    plugins.withId("java-platform") {
        configure<PublishingExtension> {
            publications.create<MavenPublication>("maven") { from(components["javaPlatform"]) }
        }
    }

    configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            pom {
                name.set(project.name)
                description.set("fhg-kit — framework-free shared library for the fhgomes constellation of micro-SaaS products (LLM usage ledger and cost estimation first; credentials, flags, audit and integration contracts to follow)")
                url.set("https://github.com/fhgomes/fhg-kit")
                licenses { license { name.set("Apache License 2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0") } }
                developers { developer { id.set("fhgomes"); name.set("Fernando Gomes") } }
                scm {
                    url.set("https://github.com/fhgomes/fhg-kit")
                    connection.set("scm:git:https://github.com/fhgomes/fhg-kit.git")
                }
            }
        }
        repositories {
            // GitHub Packages. Credentials ONLY from the environment (CI: GITHUB_ACTOR/GITHUB_TOKEN) or ~/.gradle/gradle.properties.
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/fhgomes/fhg-kit")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: (findProperty("gpr.user") as String?)
                    password = System.getenv("GITHUB_TOKEN") ?: (findProperty("gpr.key") as String?)
                }
            }
        }
    }
}
