# ADR 0004: Traefik as the ingress controller (not ingress-nginx)

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

The gateway must be reachable from outside the local kind cluster through a Kubernetes `Ingress`. `ingress-nginx` is
the controller most tutorials and kind guides reach for - but the Kubernetes project announced its retirement: best-effort maintenance only until March 2026, afterwards no releases, bug fixes or security fixes. Its
repository is archived; the last release was in March 2026.

Installing an unmaintained, internet-facing component would be poor practice, even for a local cluster that others
will copy from.

## Decision

Use **Traefik** (pinned to `v3.7.13`) as the ingress controller, installed from plain manifests in
`k8s/infrastructure/traefik` (no Helm required). It only watches standard `Ingress` resources of class `traefik`;
the application manifests stay controller-agnostic. In kind it runs on the labelled control-plane node, listens on an
unprivileged port with a `hostPort` of 80, and the cluster config maps host port 8090 to it.

## Consequences

- The application's `Ingress` is the standard `networking.k8s.io/v1` resource: switching controllers later only
  touches `k8s/infrastructure` and the `ingressClassName`.
- Traefik is actively maintained and runs as an unprivileged user with a read-only root filesystem.
- We maintain a small manifest ourselves instead of pulling a Helm chart; the trade-off is that upgrades are manual.
  Pinning the image tag keeps that explicit.
- **Follow-up:** the Kubernetes project points to the **Gateway API** as the long-term successor of `Ingress`. Migrating
  (Traefik and several other controllers support it) is a sensible next step but not required for this scope.
- There is deliberately a second "gateway" in the picture: Traefik is the cluster edge (Layer 7 entry), the Spring
  Cloud Gateway is the application's API gateway (routing, correlation ID, later JWT validation). Traefik only
  forwards everything to the latter.

## Alternatives considered

- **ingress-nginx:** retired, see above.
- **Gateway API with Envoy Gateway or NGINX Gateway Fabric:** the forward-looking choice, but a bigger step (new
  resource kinds, CRDs, controller + data plane) than this project needs today.
- **Skipping the Ingress and using `kubectl port-forward`:** simpler, but it does not demonstrate how traffic enters a cluster.
- **kind's `extraPortMappings` straight to a NodePort service:** works, but skips the Ingress layer entirely.
