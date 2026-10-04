package dev.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Proves the wiring without calling the model: the context starts with Ollama unreachable (the shop must not
 * depend on it at startup), Flyway creates the vector table, and the configured names reach the Spring AI beans.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AssistantWiringIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private OllamaChatModel chatModel;

    @Test
    void vectorStoreIsBackedByPgVector() {
        assertThat(vectorStore).isInstanceOf(PgVectorStore.class);
    }

    @Test
    void flywayCreatesVectorTableWithTheDimensionOfTheEmbeddingModel() {
        // pgvector keeps the declared dimension in the column's type modifier.
        Integer dimension = jdbcTemplate.queryForObject("""
                SELECT atttypmod FROM pg_attribute
                WHERE attrelid = 'vector_store'::regclass AND attname = 'embedding'""", Integer.class);

        assertThat(dimension).isEqualTo(768);
    }

    @Test
    void chatModelNameComesFromConfiguration() {
        assertThat(chatModel.getOptions().getModel()).isEqualTo("llama3.2:3b");
    }
}
