# Roadmap

What is built and what comes next. Each item states the outcome; the reasoning behind the larger decisions is
recorded in [`docs/adr`](docs/adr).

## Done

- Multi-module Gradle build (Kotlin DSL, version catalog, convention plugins, Java 25)
- Reference slice in `book-service`: REST API, validation, Flyway, JPA, RFC 9457 problem responses, unit, web-slice
  and Testcontainers tests
- API gateway with per-environment routing and correlation ID propagation
- `assistant-service` wiring: Spring AI with Ollama (chat + embeddings) and pgvector, `GET /assistant/ping`
- Docker Compose stack with healthchecks; Kubernetes manifests (Kustomize, kind) with probes, limits and ingress
- CI: build, test, image build and manifest validation on every push and pull request

## 1. Core services

- **`user-service`:** registration and profile with case-insensitive unique e-mail, optimistic locking, no PII in logs.
- **`order-service`:** orders as an aggregate with price snapshots per line item and paged queries without N+1 selects.

## 2. Event-driven stock and catalog index

- **`OrderPlaced` -> stock:** the `order-service` publishes the event; the `book-service` reduces stock as an
  idempotent consumer with bounded retries and a dead-letter topic. Concurrent orders never oversell.
- **`BookCreated` / `BookUpdated` -> embeddings:** the `book-service` publishes catalog changes; the `assistant-service`
  maintains the vector index from them (idempotent, tolerant of reordering and of an unavailable model) and can
  backfill books that existed before it.

## 3. AI features

- **Semantic search:** `GET /assistant/search?q=...` backed by vector search with a similarity threshold.
- **Book advisor (RAG):** `POST /assistant/recommend` answers only from the catalog and returns the book IDs it used as
  sources. Hallucinated titles are rejected in code, retrieved text is treated as untrusted input (prompt injection).
- **Tool calling:** the advisor can look up the signed-in user's orders; the user identity never comes from the model.

## 4. Reliability and quality

- **Resilience:** circuit breaker, timeout and bulkhead around every LLM call. The shop keeps working when Ollama is
  down; this is demonstrated and documented.
- **Retrieval evaluation:** a fixed question set with expected books, recall@k and MRR as an automated check, with a
  recorded baseline.

## 5. Security and observability

- **JWT authentication** at the gateway, trusted identity propagation to the services, object-level authorisation.
- **Distributed tracing** with OpenTelemetry across the gateway, services and Kafka, including LLM calls.

## 6. Scaling

- **Horizontal Pod Autoscaler** for one service, with a k6 load test that triggers and documents scale-up and
  scale-down.

## Later

Transactional outbox, saga for order compensation, hybrid (vector + full-text) search, rate limiting for LLM endpoints,
Gateway API instead of Ingress, response streaming.
