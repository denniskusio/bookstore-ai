package dev.bookstore.book.web;

import dev.bookstore.book.service.BookService;
import dev.bookstore.book.web.dto.BookResponse;
import dev.bookstore.book.web.dto.CreateBookRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/books")
class BookController {

    private final BookService bookService;

    BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /** Paged catalog. Sorting by title by default keeps page boundaries stable between requests. */
    @GetMapping
    PagedModel<BookResponse> list(@PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return new PagedModel<>(bookService.findAll(pageable));
    }

    @GetMapping("/{id}")
    BookResponse get(@PathVariable UUID id) {
        return bookService.findById(id);
    }

    @PostMapping
    ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
        BookResponse created = bookService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
