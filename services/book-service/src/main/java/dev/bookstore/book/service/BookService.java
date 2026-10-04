package dev.bookstore.book.service;

import dev.bookstore.book.domain.Book;
import dev.bookstore.book.domain.BookNotFoundException;
import dev.bookstore.book.domain.DuplicateIsbnException;
import dev.bookstore.book.repository.BookRepository;
import dev.bookstore.book.web.dto.BookResponse;
import dev.bookstore.book.web.dto.CreateBookRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction boundary of the catalog. Methods return DTOs so that entities are mapped while
 * the transaction is still open (lazy associations would otherwise fail with open-in-view disabled).
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Page<BookResponse> findAll(Pageable pageable) {
        return bookRepository.findAll(pageable).map(BookResponse::from);
    }

    public BookResponse findById(UUID id) {
        return bookRepository.findById(id)
                .map(BookResponse::from)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public BookResponse create(CreateBookRequest request) {
        // Friendly 409 for the common case. Two concurrent requests can still both pass this check;
        // the unique constraint on isbn then rejects the second one (mapped to 409 as well).
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new DuplicateIsbnException(request.isbn());
        }
        Book saved = bookRepository.save(new Book(
                request.isbn(),
                request.title(),
                request.author(),
                request.description(),
                request.price(),
                request.stock()));
        log.info("Created book id={} isbn={}", saved.getId(), saved.getIsbn());
        return BookResponse.from(saved);
    }
}
