package dev.bookstore.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bookstore.common.correlation.CorrelationId;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesIdWhenRequestHasNone() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringRequest = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), response, chainCapturingMdc(mdcDuringRequest));

        String responseId = response.getHeader(CorrelationId.HEADER);
        assertThat(responseId).isNotBlank();
        assertThat(mdcDuringRequest.get()).isEqualTo(responseId);
    }

    @Test
    void keepsValidIncomingIdAndExposesItToTheRequestThread() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, "client-supplied-42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringRequest = new AtomicReference<>();

        filter.doFilter(request, response, chainCapturingMdc(mdcDuringRequest));

        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo("client-supplied-42");
        assertThat(mdcDuringRequest.get()).isEqualTo("client-supplied-42");
    }

    @Test
    void replacesUnsafeIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, "evil id with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationId.HEADER)).isNotEqualTo("evil id with spaces");
    }

    @Test
    void clearsMdcAfterTheRequest() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcEvenWhenTheChainFails() {
        MockFilterChain failingChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws ServletException {
                throw new ServletException("boom");
            }
        };

        assertThatThrownBy(() ->
                filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), failingChain))
                .isInstanceOf(ServletException.class);

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    private static MockFilterChain chainCapturingMdc(AtomicReference<String> target) {
        return new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                target.set(MDC.get(CorrelationId.MDC_KEY));
            }
        };
    }
}
