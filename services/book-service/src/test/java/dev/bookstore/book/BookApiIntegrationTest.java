package dev.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bookstore.book.domain.Book;
import dev.bookstore.book.repository.BookRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Full vertical slice against a real PostgreSQL: HTTP -> validation -> service -> JPA -> Flyway-created schema.
 * Starting the context also proves that the entity mapping matches the migration ({@code ddl-auto: validate}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookApiIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void cleanDatabase() {
        bookRepository.deleteAll();
    }

    @Test
    void createdBookCanBeReadBackById() {
        MvcTestResult created = post(bookJson("9780132350884", "Clean Code", 5));

        assertThat(created).hasStatus(HttpStatus.CREATED);
        // The create response must be complete by itself, not only what a later GET returns.
        assertThat(created).bodyJson()
                .hasPathSatisfying("$.createdAt", createdAt -> createdAt.assertThat().isNotNull())
                .hasPathSatisfying("$.updatedAt", updatedAt -> updatedAt.assertThat().isNotNull());
        String location = created.getResponse().getHeader("Location");
        assertThat(location).startsWith("http://localhost/books/");

        assertThat(mvc.get().uri(location))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.isbn", isbn -> isbn.assertThat().isEqualTo("9780132350884"))
                .hasPathSatisfying("$.title", title -> title.assertThat().isEqualTo("Clean Code"))
                .hasPathSatisfying("$.stock", stock -> stock.assertThat().isEqualTo(5))
                .hasPathSatisfying("$.createdAt", createdAt -> createdAt.assertThat().isNotNull());

        Book stored = bookRepository.findAll().getFirst();
        assertThat(stored.getVersion()).isZero();
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
    }

    @Test
    void duplicateIsbnIsRejectedWith409() {
        assertThat(post(bookJson("9780132350884", "Clean Code", 5))).hasStatus(HttpStatus.CREATED);

        assertThat(post(bookJson("9780132350884", "Another Title", 1)))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(bookRepository.count()).isEqualTo(1);
    }

    /**
     * Several requests race to insert the same ISBN: the existence check in the service cannot catch all of them,
     * so the unique constraint has to. Whatever the interleaving, exactly one wins and nobody gets a 500.
     */
    @Test
    void concurrentCreatesWithSameIsbnYieldExactlyOneBookAndNoServerError() throws Exception {
        int parallelRequests = 8;
        String json = bookJson("9780201633610", "Design Patterns", 3);
        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < parallelRequests; i++) {
                futures.add(executor.submit(() -> {
                    startSignal.await();
                    return post(json).getResponse().getStatus();
                }));
            }
            startSignal.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }

            assertThat(statuses).containsOnly(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());
            assertThat(statuses.stream().filter(status -> status == HttpStatus.CREATED.value())).hasSize(1);
            assertThat(bookRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void invalidPayloadIsRejectedWith400AndNothingIsStored() {
        assertThat(post("""
                {"isbn":"abc","title":"","author":"X","price":-5,"stock":-1}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);

        assertThat(bookRepository.count()).isZero();
    }

    @Test
    void unknownBookYields404Problem() {
        assertThat(mvc.get().uri("/books/{id}", UUID.randomUUID()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void listIsPagedAndSortedByTitle() {
        post(bookJson("9780201633610", "Design Patterns", 3));
        post(bookJson("9780321125217", "Domain-Driven Design", 2));
        post(bookJson("9780132350884", "Clean Code", 5));

        assertThat(mvc.get().uri("/books?size=2"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content[*].title").asArray()
                .containsExactly("Clean Code", "Design Patterns");

        assertThat(mvc.get().uri("/books?size=2"))
                .bodyJson()
                .hasPathSatisfying("$.page.totalElements", total -> total.assertThat().isEqualTo(3))
                .hasPathSatisfying("$.page.totalPages", pages -> pages.assertThat().isEqualTo(2));

        assertThat(mvc.get().uri("/books?size=2&page=1"))
                .bodyJson()
                .extractingPath("$.content[*].title").asArray()
                .containsExactly("Domain-Driven Design");
    }

    @Test
    void unknownSortPropertyYields400() {
        assertThat(mvc.get().uri("/books?sort=doesNotExist"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void databaseRejectsNegativeStockEvenWhenValidationIsBypassed() {
        Book invalid = new Book("9780132350884", "Clean Code", "Robert C. Martin", null, new BigDecimal("10.00"), -1);

        assertThatThrownBy(() -> bookRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private MvcTestResult post(String json) {
        return mvc.post().uri("/books").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String bookJson(String isbn, String title, int stock) {
        return """
                {"isbn":"%s","title":"%s","author":"Some Author","description":"About %s","price":29.90,"stock":%d}"""
                .formatted(isbn, title, title, stock);
    }
}
