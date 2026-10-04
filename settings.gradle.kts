rootProject.name = "bookstore-ai"

pluginManagement {
    // Convention plugins (bookstore.*) live in an included build and are resolved by plugin id.
    includeBuild("build-logic")
}

plugins {
    // Lets Gradle download the JDK requested by the toolchain (Java 25) if it is not installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // Repositories are declared once, here - not per project.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

include(
    ":libs:service-common",
    ":services:api-gateway",
    ":services:user-service",
    ":services:book-service",
    ":services:order-service",
    ":services:assistant-service",
)
