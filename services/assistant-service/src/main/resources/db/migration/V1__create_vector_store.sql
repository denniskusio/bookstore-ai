CREATE EXTENSION IF NOT EXISTS vector;

-- Layout expected by Spring AI's PgVectorStore (defaults: uuid ids, 768 dimensions, cosine distance).
-- The id is the book's UUID, so re-indexing a book overwrites its previous embedding.
CREATE TABLE vector_store (
    id        UUID PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(768)
);

-- Approximate nearest-neighbour index for cosine distance (matches distance-type: cosine-distance).
CREATE INDEX vector_store_embedding_idx ON vector_store USING hnsw (embedding vector_cosine_ops);
