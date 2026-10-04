# ADR 0002: pgvector in PostgreSQL instead of a dedicated vector database

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

Semantic search and RAG need to store embeddings (vectors) and find the nearest ones to a query vector. Options
are a dedicated vector database (Qdrant, Milvus, Weaviate, Chroma, ...) or a vector extension in a database we
already run. The catalog is small (thousands of books, not billions of vectors), and PostgreSQL is already part of
the stack.

## Decision

Embeddings are stored in PostgreSQL with the **pgvector** extension, in the assistant-service's own database
(`assistant_db`), using Spring AI's `PgVectorStore`. The table is created by a Flyway migration (768 dimensions,
HNSW index, cosine distance), not by Spring AI at startup, so schema changes stay reviewable and versioned like
every other schema change.

## Consequences

- **One system less** to operate, secure, back up and run locally. The same container image
  (`pgvector/pgvector`) serves Docker Compose, Kubernetes and the Testcontainers integration tests.
- **Relational and vector data live together.** Metadata filters are plain SQL, and the vector table can be
  joined, constrained and backed up like any other table - useful for RAG, where retrieval is usually "nearest
  vectors *and* some condition".
- **Transactional.** Upserting a book's embedding is an ordinary transaction.
- **Limits are known and acceptable here.** At very large scale (hundreds of millions of vectors, heavy write load,
  advanced filtering) a dedicated engine scales and tunes better. That is far beyond a bookstore catalog.
- **Coupling to the embedding model.** The column dimension (768) is fixed by `nomic-embed-text`. Changing the model
  means a migration plus re-embedding all books; the assistant-service's wiring test checks that configuration and
  schema agree.
- The assistant-service holds a *derived copy* of book data (built from `BookCreated`/`BookUpdated` events), never
  reading the book-service's database - the "database per service" rule is kept.

## Alternatives considered

- **Qdrant / Milvus / Weaviate:** excellent engines, but another stateful system with its own operations, backup
  and security story, for a data volume that does not need it.
- **Chroma / in-memory `SimpleVectorStore`:** fine for demos, not durable or realistic enough for the point of this project.
- **OpenSearch / Elasticsearch with vector support:** attractive if we also needed full-text search and aggregations; much heavier.
