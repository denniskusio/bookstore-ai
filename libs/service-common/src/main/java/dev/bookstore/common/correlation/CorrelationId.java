package dev.bookstore.common.correlation;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Names and rules for the correlation ID that ties together all log lines and responses of one
 * request across the gateway and the services. Plain Java on purpose: both the servlet filter
 * (MVC services) and the gateway's reactive filter use it.
 */
public final class CorrelationId {

    /** HTTP header carrying the ID between client, gateway and services. */
    public static final String HEADER = "X-Correlation-Id";

    /** Key under which the ID is stored in the logging MDC. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Accepted shape of an incoming ID. The ID ends up in log lines, so anything that could forge or
     * break a line (whitespace, newlines, control characters) is rejected, and the length is capped.
     */
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private CorrelationId() {
    }

    /** Returns {@code incoming} if it is acceptable, otherwise a freshly generated ID. */
    public static String resolve(String incoming) {
        if (incoming != null && VALID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}
