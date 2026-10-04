package dev.bookstore.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Every environment profile must start, point the routes at the right upstream and trust the callers that
 * actually reach the gateway in that environment. The other tests only run the default profile, so a mistake
 * that exists only in the {@code docker} or {@code k8s} part of application.yml (a duplicate key, a typo in a
 * host name, a regex that does not match the real client address) would otherwise surface first in a deployment.
 */
class ProfileConfigurationTests {

    /**
     * The sample address is what the gateway sees as the caller in that environment: the loopback address for
     * local runs, the Compose network's gateway address for Docker Desktop (observed: 172.18.0.1), and a pod
     * address of the ingress controller in kind's pod network.
     */
    @ParameterizedTest(name = "profile {0} routes books to {1} and trusts caller {2}")
    @CsvSource({
            "local,  http://localhost:8082,                                         127.0.0.1",
            "docker, http://book-service:8082,                                      172.18.0.1",
            "k8s,    http://book-service.bookstore.svc.cluster.local:8082,          10.244.0.7"
    })
    @ResourceLock("spring-application-context")
    void profileStartsRoutesCorrectlyAndTrustsItsCallers(String profile, String expectedBookUpstream, String caller) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(GatewayApplication.class)
                .web(WebApplicationType.REACTIVE)
                .profiles(profile)
                // Command-line arguments outrank application.yml (a plain properties(...) call would not), and
                // port 0 keeps the test independent of whatever already listens on 8080.
                .run("--server.port=0")) {

            assertThat(context.getEnvironment().getProperty("bookstore.upstream.book")).isEqualTo(expectedBookUpstream);

            // Spring Cloud Gateway sets Forwarded / X-Forwarded-* only for callers matching this regex.
            String trustedProxies = context.getEnvironment()
                    .getProperty("spring.cloud.gateway.server.webflux.trusted-proxies");
            assertThat(caller).matches(trustedProxies);

            // Without an explicit value Spring Boot switches to "native" on Kubernetes, and Reactor Netty then
            // reports the forwarded client (not the ingress) as the caller - so the regex above never matches.
            assertThat(context.getEnvironment().getProperty("server.forward-headers-strategy")).isEqualTo("none");
        }
    }
}
