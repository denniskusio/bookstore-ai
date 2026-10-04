rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        // Reuse the repository-wide catalog so build-logic and the services never disagree.
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
