plugins {
    id("bookstore.spring-service-conventions")
}

dependencies {
    // Only the servlet-free CorrelationId rules are used here; the servlet filter in the same library
    // stays inactive because the gateway is a reactive application.
    implementation(project(":libs:service-common"))

    // Spring Cloud is not managed by the Boot BOM, so its release train is imported explicitly.
    implementation(platform(libs.spring.cloud.dependencies))
    // Reactive (WebFlux) Gateway; brings spring-boot-starter-webflux transitively.
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")

    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
}
