# ADR 0003: Ollama as the local LLM runtime

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

The assistant needs a chat model (for the advisor) and an embedding model (for search). The project must run on a
developer laptop without a GPU and in CI-like environments - without anyone needing an
account, an API key or a credit card, and without sending catalog or order data to a third party.

## Decision

Models run locally in **Ollama**, accessed through Spring AI's `ChatClient` / `EmbeddingModel` abstractions.

- Chat model: `llama3.2:3b` (about 2 GB, runs on CPU, supports tool calling - needed for the advisor's tool calling).
- Embedding model: `nomic-embed-text` (768 dimensions).
- Base URL and both model names are configuration (`OLLAMA_BASE_URL`, `OLLAMA_CHAT_MODEL`,
  `OLLAMA_EMBEDDING_MODEL`); no API keys exist anywhere.
- Ollama runs as a container (Docker Compose, Kubernetes) with an init step that pulls the models, **or** natively on
  the host (`docker-compose.native-ollama.yml`), which is much faster on macOS because the container cannot use the
  Apple GPU.
- The assistant-service does not need Ollama at startup, and its readiness does not depend on it: the shop must keep
  working when the LLM is unavailable (see the resilience item in [`ROADMAP.md`](../../ROADMAP.md)).

## Consequences

- **Zero setup friction and no cost**; data never leaves the machine; results are reproducible with pinned model tags.
- **Weaker and slower than hosted frontier models.** A 3B model on CPU answers in seconds, follows instructions less
  reliably and hallucinates more. This is a feature for the project's goals: grounding, validation and evaluation
  (the RAG advisor and the retrieval evaluation) matter *more* with a modest model.
- **Tool-calling support varies per model**, so the chat model is chosen with that in mind.
- **Swapping providers is a configuration and dependency change**, not a rewrite, because application code talks to
  Spring AI abstractions. Embeddings are the exception: vectors from different models are incompatible, so changing
  the embedding model means re-embedding everything.
- The models are large downloads (a few GB) on first start; they are stored in a volume and downloaded once.

## Alternatives considered

- **Hosted APIs (OpenAI, Anthropic, ...):** stronger models, but need keys and billing, send data to a third party and
  make the project non-reproducible for reviewers.
- **llama.cpp / vLLM directly:** more control over serving, more operational work, no model management.
- **Docker Model Runner:** convenient with Docker Desktop, but ties the project to that product; Ollama is portable
  across Compose, Kubernetes and bare metal.
