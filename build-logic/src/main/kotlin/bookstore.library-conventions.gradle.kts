import org.springframework.boot.gradle.plugin.SpringBootPlugin

/**
 * Setup for internal libraries shared by services. Unlike a service, a library is a plain jar:
 * it gets the Spring Boot BOM for consistent versions but does not apply the Boot plugin.
 */
plugins {
    `java-library`
    id("bookstore.java-conventions")
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
