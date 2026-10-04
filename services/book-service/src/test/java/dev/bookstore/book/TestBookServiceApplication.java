package dev.bookstore.book;

import org.springframework.boot.SpringApplication;

/**
 * Dev-time launcher: runs the service against a throwaway Testcontainers PostgreSQL,
 * so no Docker Compose stack is needed. Start it with {@code ./gradlew :services:book-service:bootTestRun}.
 */
public class TestBookServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(BookServiceApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
