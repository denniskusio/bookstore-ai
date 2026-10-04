plugins {
    id("bookstore.spring-service-conventions")
}

dependencies {
    implementation(project(":libs:service-common"))

    // Spring AI is not managed by the Boot BOM, so its BOM is imported explicitly.
    implementation(platform(libs.spring.ai.bom))
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // PgVectorStore works on a plain JdbcTemplate, so no JPA here.
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    // Kafka client for the planned BookCreated / BookUpdated consumer (see ROADMAP.md).
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
}
