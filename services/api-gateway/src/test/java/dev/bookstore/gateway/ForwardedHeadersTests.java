package dev.bookstore.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
 * Services build absolute URLs (for example the Location header of a created resource) from the request they
 * see. Behind the gateway that request carries the internal service host, so the gateway has to tell the
 * service which host the client actually used. Spring Cloud Gateway only does that once
 * {@code trusted-proxies} is configured - this test guards that setting.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ForwardedHeadersTests {

    private static final AtomicReference<String> forwardedHost = new AtomicReference<>();
    private static final AtomicReference<String> forwardedProto = new AtomicReference<>();
    private static HttpServer upstream;

    @DynamicPropertySource
    static void routeBooksToStub(DynamicPropertyRegistry registry) throws IOException {
        upstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        upstream.createContext("/books", exchange -> {
            forwardedHost.set(exchange.getRequestHeaders().getFirst("X-Forwarded-Host"));
            forwardedProto.set(exchange.getRequestHeaders().getFirst("X-Forwarded-Proto"));
            byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
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
        forwardedHost.set(null);
        forwardedProto.set(null);
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void upstreamLearnsWhichHostTheClientUsed() {
        client.get().uri("/books")
                .header("Host", "shop.example.com")
                .exchange()
                .expectStatus().isOk();

        assertThat(forwardedHost.get()).isEqualTo("shop.example.com");
        assertThat(forwardedProto.get()).isEqualTo("http");
    }
}
