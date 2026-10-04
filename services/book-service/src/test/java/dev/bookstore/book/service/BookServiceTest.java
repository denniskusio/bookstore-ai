package dev.bookstore.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.bookstore.book.domain.Book;
import dev.bookstore.book.domain.BookNotFoundException;
import dev.bookstore.book.domain.DuplicateIsbnException;
import dev.bookstore.book.repository.BookRepository;
import dev.bookstore.book.web.dto.BookResponse;
import dev.bookstore.book.web.dto.CreateBookRequest;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pure unit test: no Spring context, the repository is a mock. */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    private static final String ISBN = "9780132350884";

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void createStoresBookAndReturnsItsRepresentation() {
        given(bookRepository.existsByIsbn(ISBN)).willReturn(false);
        given(bookRepository.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

        BookResponse response = bookService.create(request());

        assertThat(response.isbn()).isEqualTo(ISBN);
        assertThat(response.title()).isEqualTo("Clean Code");
        assertThat(response.stock()).isEqualTo(5);
    }

    @Test
    void createRejectsDuplicateIsbnWithoutSaving() {
        given(bookRepository.existsByIsbn(ISBN)).willReturn(true);

        assertThatThrownBy(() -> bookService.create(request()))
                .isInstanceOf(DuplicateIsbnException.class)
                .hasMessageContaining(ISBN);

        verify(bookRepository, never()).save(any());
    }

    @Test
    void findByIdThrowsWhenBookDoesNotExist() {
        UUID id = UUID.randomUUID();
        given(bookRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(id))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    private static CreateBookRequest request() {
        return new CreateBookRequest(ISBN, "Clean Code", "Robert C. Martin", "A handbook of agile craftsmanship",
                new BigDecimal("34.99"), 5);
    }
}
