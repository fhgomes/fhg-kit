// Bill of materials: consumers import this platform and omit versions on the individual modules.
plugins {
    `java-platform`
}

dependencies {
    constraints {
        rootProject.subprojects
            .filter { it.path != project.path }
            .forEach { api(project(it.path)) }
    }
}
