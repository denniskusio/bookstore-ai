plugins {
    `kotlin-dsl`
}

dependencies {
    // Puts the Spring Boot Gradle plugin on the classpath of the convention plugins,
    // so they can apply it with id("org.springframework.boot") and reference SpringBootPlugin.
    implementation(libs.spring.boot.gradle.plugin)
}
