package dev.bookstore.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import dev.bookstore.common.correlation.CorrelationId;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Verifies the correlation ID contract end to end through a real gateway route. A tiny stub plays the
 * book-service: it records the header it received and echoes it back, exactly as the servlet filter would.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorrelationIdPropagationTests {

    private static final AtomicReference<String> headerSeenByUpstream = new AtomicReference<>();
    private static HttpServer upstream;

    @DynamicPropertySource
    static void routeBooksToStub(DynamicPropertyRegistry registry) throws IOException {
        upstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        upstream.createContext("/books", exchange -> {
            String received = exchange.getRequestHeaders().getFirst(CorrelationId.HEADER);
            headerSeenByUpstream.set(received);
            byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            if (received != null) {
                exchange.getResponseHeaders().add(CorrelationId.HEADER, received);
            }
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        upstream.start();
        registry.add("bookstore.upstream.book", () -> "http://localhost:" + upstream.getAddress().getPort());
    }

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @LocalServerPort
    private int port;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        headerSeenByUpstream.set(null);
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void generatesIdWhenClientSendsNone() {
        List<String> responseValues = client.get().uri("/books")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseHeaders().get(CorrelationId.HEADER);

        assertThat(responseValues).hasSize(1);
        assertThat(UUID.fromString(responseValues.getFirst())).isNotNull();
        assertThat(headerSeenByUpstream.get()).isEqualTo(responseValues.getFirst());
    }

    @Test
    void forwardsClientSuppliedIdUnchangedAndReturnsItOnce() {
        List<String> responseValues = client.get().uri("/books")
                .header(CorrelationId.HEADER, "client-trace-42")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseHeaders().get(CorrelationId.HEADER);

        assertThat(headerSeenByUpstream.get()).isEqualTo("client-trace-42");
        // The upstream echoes the header as well; the gateway must not turn that into two values.
        assertThat(responseValues).containsExactly("client-trace-42");
    }

    @Test
    void replacesUnsafeClientSuppliedId() {
        client.get().uri("/books")
                .header(CorrelationId.HEADER, "not valid: spaces & symbols")
                .exchange()
                .expectStatus().isOk();

        assertThat(headerSeenByUpstream.get())
                .isNotEqualTo("not valid: spaces & symbols")
                .satisfies(id -> assertThat(UUID.fromString(id)).isNotNull());
    }

    @Test
    void unroutedRequestsAndActuatorAlsoCarryTheId() {
        client.get().uri("/does-not-exist")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().exists(CorrelationId.HEADER);

        client.get().uri("/actuator/health/liveness")
                .header(CorrelationId.HEADER, "probe-1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationId.HEADER, "probe-1");
    }
}
