plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val flywayVersion = "9.22.3"

dependencies {
    implementation(project(":fore-common"))

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:${flywayVersion}")

    // JSON / JSONB support
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.test {
    useJUnitPlatform()
}
