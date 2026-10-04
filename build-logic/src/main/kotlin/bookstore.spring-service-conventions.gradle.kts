import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.bundling.BootJar

/**
 * Common setup for every deployable Spring Boot service: the Boot BOM, the runnable jar,
 * and the observability baseline (Actuator probes + Micrometer metrics with a Prometheus endpoint).
 */
plugins {
    id("bookstore.java-conventions")
    id("org.springframework.boot")
}

dependencies {
    // The Boot BOM supplies the versions for everything below (and for what services add).
    // `implementation` is inherited by testImplementation / testRuntimeClasspath, so tests are covered too.
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-micrometer-metrics")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Gradle 9 no longer puts the JUnit launcher on the test runtime classpath implicitly.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<BootJar>("bootJar") {
    // Stable file name so Dockerfiles do not have to know the version.
    archiveFileName = "${project.name}.jar"
}
