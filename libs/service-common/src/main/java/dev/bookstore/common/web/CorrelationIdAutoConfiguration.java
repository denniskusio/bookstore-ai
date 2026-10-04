package dev.bookstore.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Registers the {@link CorrelationIdFilter} in every servlet-based service that has this library on its
 * classpath - nothing to configure. (The reactive gateway has its own filter and ignores this class.)
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class CorrelationIdAutoConfiguration {

    /** Runs first, so even logs and errors produced by other filters carry the ID. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}
