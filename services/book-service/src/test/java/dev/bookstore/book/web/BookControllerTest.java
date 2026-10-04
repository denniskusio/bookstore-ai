package dev.bookstore.book.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import dev.bookstore.book.domain.BookNotFoundException;
import dev.bookstore.book.domain.DuplicateIsbnException;
import dev.bookstore.book.service.BookService;
import dev.bookstore.book.web.dto.BookResponse;
import dev.bookstore.book.web.dto.CreateBookRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Web-layer slice test: real Spring MVC (validation, JSON, exception handling), mocked service.
 * Fast, needs no database, and pins down the HTTP contract including the error format.
 */
@WebMvcTest(BookController.class)
class BookControllerTest {

    private static final String VALID_BODY = """
            {"isbn":"9780132350884","title":"Clean Code","author":"Robert C. Martin",
             "description":"A handbook","price":34.99,"stock":5}""";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private BookService bookService;

    @Test
    void createReturns201WithLocationHeader() {
        UUID id = UUID.randomUUID();
        given(bookService.create(any(CreateBookRequest.class))).willReturn(response(id));

        assertThat(mvc.post().uri("/books").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .hasStatus(HttpStatus.CREATED)
                .headers().hasValue("Location", "http://localhost/books/" + id);
    }

    @Test
    void createRejectsInvalidPayloadWithFieldErrors() {
        String invalid = """
                {"isbn":"123","title":"","author":"Someone","price":-1,"stock":-3}""";

        assertThat(mvc.post().uri("/books").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.title", title -> title.assertThat().isEqualTo("Validation failed"))
                .extractingPath("$.errors[*].field").asArray()
                .containsExactlyInAnyOrder("isbn", "title", "price", "stock");
    }

    @Test
    void createRejectsMissingRequiredFields() {
        assertThat(mvc.post().uri("/books").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[*].field").asArray()
                .contains("isbn", "title", "author", "price", "stock");
    }

    @Test
    void createRejectsMalformedJson() {
        assertThat(mvc.post().uri("/books").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void createMapsDuplicateIsbnTo409() {
        given(bookService.create(any(CreateBookRequest.class)))
                .willThrow(new DuplicateIsbnException("9780132350884"));

        assertThat(mvc.post().uri("/books").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.title", title -> title.assertThat().isEqualTo("Duplicate ISBN"));
    }

    @Test
    void getMapsMissingBookTo404() {
        UUID id = UUID.randomUUID();
        given(bookService.findById(id)).willThrow(new BookNotFoundException(id));

        assertThat(mvc.get().uri("/books/{id}", id))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.title", title -> title.assertThat().isEqualTo("Book not found"))
                .hasPathSatisfying("$.instance", instance -> instance.assertThat().isEqualTo("/books/" + id));
    }

    @Test
    void getRejectsMalformedId() {
        assertThat(mvc.get().uri("/books/not-a-uuid"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void unexpectedFailureYieldsGeneric500WithoutLeakingDetails() {
        UUID id = UUID.randomUUID();
        given(bookService.findById(id)).willThrow(new IllegalStateException("secret internal state"));

        assertThat(mvc.get().uri("/books/{id}", id))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyText().doesNotContain("secret internal state");
    }

    private static BookResponse response(UUID id) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new BookResponse(id, "9780132350884", "Clean Code", "Robert C. Martin", "A handbook",
                new BigDecimal("34.99"), 5, now, now);
    }
}
