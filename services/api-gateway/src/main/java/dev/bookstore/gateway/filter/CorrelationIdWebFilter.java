package dev.bookstore.gateway.filter;

import dev.bookstore.common.correlation.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Entry point of the correlation ID: reuses a well-formed ID sent by the client or creates one, forwards it
 * to the upstream service and returns it on the response.
 *
 * <p>This is a {@link WebFilter} rather than a gateway filter so that it also covers requests that match no
 * route (404) and the gateway's own Actuator endpoints. Unlike the servlet services, the gateway does not
 * use the logging MDC (a request hops between threads in a reactive pipeline), so the ID is logged explicitly.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = CorrelationId.resolve(exchange.getRequest().getHeaders().getFirst(CorrelationId.HEADER));

        // header(...) replaces any value the client sent, so an unsafe incoming ID never reaches the services.
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(CorrelationId.HEADER, correlationId)
                .build();

        // Services echo the header too. Setting it right before the response is committed replaces
        // that value instead of adding a second one.
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(CorrelationId.HEADER, correlationId);
            return Mono.empty();
        });

        log.info("{} {} correlationId={}", request.getMethod(), request.getPath(), correlationId);
        return chain.filter(exchange.mutate().request(request).build());
    }
}
