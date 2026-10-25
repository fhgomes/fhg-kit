plugins {
    `java-library`
}

description = "fhg-kit — Spring Boot auto-configuration (the only module that knows Spring)"

dependencies {
    // Boot BOM only for version alignment; the starter is compiled against Boot but ships none of it.
    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}"))
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}"))

    api(project(":fhg-kit-llm"))

    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
