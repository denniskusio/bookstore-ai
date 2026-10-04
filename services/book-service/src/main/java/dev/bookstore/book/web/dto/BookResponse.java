package dev.bookstore.book.web.dto;

import dev.bookstore.book.domain.Book;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Representation of a {@link Book} on the REST API. The entity itself never leaves the service layer. */
public record BookResponse(
        UUID id,
        String isbn,
        String title,
        String author,
        String description,
        BigDecimal price,
        int stock,
        Instant createdAt,
        Instant updatedAt) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getPrice(),
                book.getStock(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }
}
