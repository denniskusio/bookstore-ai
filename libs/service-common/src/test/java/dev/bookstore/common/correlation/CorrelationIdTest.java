package dev.bookstore.common.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CorrelationIdTest {

    @ParameterizedTest
    @ValueSource(strings = {"abc123", "8f14e45f-ceea-467f-a0e6-2b3c4d5e6f70", "req_42.v2", "A"})
    void keepsWellFormedIncomingIds(String incoming) {
        assertThat(CorrelationId.resolve(incoming)).isEqualTo(incoming);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "has space", "line\nbreak", "tab\there", "<script>", "ümlaut", "a/b"})
    void replacesMissingOrUnsafeIds(String incoming) {
        String resolved = CorrelationId.resolve(incoming);

        assertThat(resolved).isNotEqualTo(incoming);
        assertThat(UUID.fromString(resolved)).isNotNull();
    }

    @Test
    void rejectsIdsLongerThan64Characters() {
        String tooLong = "a".repeat(65);

        assertThat(CorrelationId.resolve(tooLong)).isNotEqualTo(tooLong);
        assertThat(CorrelationId.resolve("a".repeat(64))).hasSize(64);
    }

    @Test
    void generatesDifferentIdsEachTime() {
        assertThat(CorrelationId.resolve(null)).isNotEqualTo(CorrelationId.resolve(null));
    }
}
