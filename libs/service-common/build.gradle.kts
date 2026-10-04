plugins {
    id("bookstore.library-conventions")
}

dependencies {
    // Everything framework-related is compileOnly: the consuming service brings its own Spring Boot.
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework:spring-web")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.slf4j:slf4j-api")

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework:spring-web")
}
