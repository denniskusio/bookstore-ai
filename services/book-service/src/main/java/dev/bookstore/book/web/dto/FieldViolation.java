package dev.bookstore.book.web.dto;

/** One rejected field in a validation error response (the {@code errors} extension of the ProblemDetail). */
public record FieldViolation(String field, String message) {
}
