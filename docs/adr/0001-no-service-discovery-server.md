# ADR 0001: No service discovery server (no Eureka)

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

The services call each other (the gateway routes to all of them) and must find each other's network address.
The classic Spring Cloud answer is a registry such as Eureka: every service registers itself on startup, and
clients look addresses up and balance load on the client side.

This project only ever runs on platforms that already provide service discovery:

- **Docker Compose** gives every service a DNS name (`book-service`) on the project network.
- **Kubernetes** gives every `Service` a stable DNS name (`book-service.bookstore.svc.cluster.local`) and a virtual
  IP that load-balances across the ready pods. Readiness probes decide which pods are part of it.

## Decision

We do not run a discovery server. Services address each other by DNS name. The names differ per environment, so
they live in Spring profiles (`local`, `docker`, `k8s`) of the gateway: its routes are identical everywhere and only
the upstream base URLs (`bookstore.upstream.*`) change.

## Consequences

- **Fewer moving parts.** No extra service to build, secure, scale, monitor and keep highly available, and no
  registration lifecycle (heartbeats, eviction delay, stale entries pointing at dead instances) to understand.
- **One source of truth.** Kubernetes already knows which pods are healthy; a second registry would be a second,
  possibly disagreeing, opinion.
- **Load balancing moves to the platform** (kube-proxy / Compose DNS round-robin), which balances per connection.
  That is fine for short HTTP requests; long-lived multiplexed connections (gRPC, HTTP/2) would need extra care.
- **The code is less portable.** Running on a plain VM fleet would need a different mechanism (DNS, Consul, ...).
  This is acceptable: the target platforms are containers.
- Service-to-service URLs are configuration, so adding a service means adding a route and an upstream property.

## Alternatives considered

- **Eureka / Spring Cloud Netflix:** valid on bare VMs or when there is no platform-level discovery, but redundant here.
- **Spring Cloud Kubernetes discovery client:** reads the Kubernetes API instead of using DNS. More power (it can
  see individual pods and labels) but needs RBAC and couples the code to Kubernetes; DNS is enough.
- **Consul / etcd:** a general-purpose registry with the same redundancy argument.
