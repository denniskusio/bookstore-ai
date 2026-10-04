package dev.bookstore.book.web;

import dev.bookstore.book.domain.BookNotFoundException;
import dev.bookstore.book.domain.DuplicateIsbnException;
import dev.bookstore.book.web.dto.FieldViolation;
import dev.bookstore.common.correlation.CorrelationId;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every failure into an RFC 9457 {@code application/problem+json} response.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} covers all Spring MVC exceptions (malformed JSON,
 * wrong types, unsupported methods, unknown paths, ...). This class adds the domain exceptions and a
 * last-resort handler that never leaks internals to the client.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BookNotFoundException.class)
    ProblemDetail handleBookNotFound(BookNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Book not found", "book-not-found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateIsbnException.class)
    ProblemDetail handleDuplicateIsbn(DuplicateIsbnException ex) {
        return problem(HttpStatus.CONFLICT, "Duplicate ISBN", "duplicate-isbn", ex.getMessage());
    }

    /** Safety net for races the service-level checks cannot see, e.g. two concurrent inserts of one ISBN. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return problem(HttpStatus.CONFLICT, "Data conflict", "data-conflict",
                "The request conflicts with existing data.");
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleUnknownSortProperty(PropertyReferenceException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid sort property", "invalid-sort-property",
                "Cannot sort by unknown property '" + ex.getPropertyName() + "'.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "internal-error",
                "An unexpected error occurred.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail body = ex.getBody();
        body.setTitle("Validation failed");
        body.setType(problemType("validation-failed"));
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** Runs for every exception Spring MVC handles itself, so those problem responses get the correlation ID too. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            addCorrelationId(problem);
        }
        return response;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(problemType(type));
        addCorrelationId(problem);
        return problem;
    }

    /** Lets a client quote the ID when reporting a problem; it is the key to all log lines of that request. */
    private static void addCorrelationId(ProblemDetail problem) {
        String correlationId = MDC.get(CorrelationId.MDC_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
    }

    private static URI problemType(String type) {
        return URI.create("urn:problem-type:" + type);
    }
}
