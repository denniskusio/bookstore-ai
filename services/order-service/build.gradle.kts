plugins {
    id("bookstore.jpa-service-conventions")
}

dependencies {
    implementation(project(":libs:service-common"))

    // Kafka client for the planned OrderPlaced event (see ROADMAP.md).
    implementation("org.springframework.boot:spring-boot-starter-kafka")
}
