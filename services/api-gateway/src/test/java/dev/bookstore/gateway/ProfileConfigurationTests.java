package dev.bookstore.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Every environment profile must start and point the routes at the right upstream. The other tests only run the
 * default profile, so a mistake that exists only in the {@code docker} or {@code k8s} part of application.yml
 * (a duplicate key, a typo in a host name) would otherwise surface first in a deployed cluster.
 */
class ProfileConfigurationTests {

    @ParameterizedTest(name = "profile {0} routes books to {1}")
    @CsvSource({
            "local,  http://localhost:8082",
            "docker, http://book-service:8082",
            "k8s,    http://book-service.bookstore.svc.cluster.local:8082"
    })
    @ResourceLock("spring-application-context")
    void profileStartsAndResolvesItsUpstreams(String profile, String expectedBookUpstream) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(GatewayApplication.class)
                .web(WebApplicationType.REACTIVE)
                .profiles(profile)
                // Command-line arguments outrank application.yml (a plain properties(...) call would not), and
                // port 0 keeps the test independent of whatever already listens on 8080.
                .run("--server.port=0")) {

            assertThat(context.getEnvironment().getProperty("bookstore.upstream.book")).isEqualTo(expectedBookUpstream);
            // Without trusted-proxies the gateway adds no X-Forwarded-* headers (see ForwardedHeadersTests).
            assertThat(context.getEnvironment().getProperty("spring.cloud.gateway.server.webflux.trusted-proxies"))
                    .isNotBlank();
        }
    }
}
