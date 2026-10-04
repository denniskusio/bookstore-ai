import org.gradle.api.tasks.testing.logging.TestExceptionFormat

/**
 * Baseline for every Java module: toolchain, encoding and the JUnit platform.
 * Framework-specific setup lives in the plugins that build on this one.
 */
plugins {
    java
}

group = "dev.bookstore"
version = "0.1.0-SNAPSHOT"

// Precompiled script plugins get no type-safe `libs` accessor, so look the catalog up by name.
val libs = the<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        exceptionFormat = TestExceptionFormat.FULL
    }
}
