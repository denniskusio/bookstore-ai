package dev.bookstore.book.domain;

import java.util.UUID;

public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(UUID id) {
        super("No book with id " + id);
    }
}
