plugins {
    `java-library`
}

dependencies {
    // Minimal dependencies for shared types
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")
    
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
