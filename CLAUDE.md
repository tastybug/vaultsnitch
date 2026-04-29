# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**VaultSnitch** is a Prometheus exporter for HashiCorp Vault. It connects to a Vault instance, enumerates accessible secrets, runs policy checks, and exposes metrics at `/metrics` for Prometheus scraping. Typical deployment integrates with Grafana dashboards and AlertManager for team notifications.

## Build & Test Commands

```bash
# Build fat JAR
./mvnw clean package

# Run all tests (unit + integration)
./mvnw clean verify

# Run only unit tests
./mvnw clean test

# Run a single test class
./mvnw test -Dtest=VaultIT

# Run a single test method
./mvnw test -Dtest=VaultIT#canEnumerateAllSecrets

# Run the application
VAULT_TOKEN=myroot java -jar target/vaultsnitch-1.0-SNAPSHOT.jar
```

Integration tests (files matching `**/*IT.java`) use TestContainers to spin up a real Vault instance — a container runtime (Docker, OrbStack, or Podman via `podman machine`) must be running locally. For Podman, also export `DOCKER_HOST` before running tests (see README).

## Architecture

The application runs a fixed-interval evaluation loop (default 300s):

1. **Collection phase** — `CollectStores` discovers all KV v2 mount points; `CollectStoreContents` recursively retrieves secrets and metadata from each store.
2. **Evaluation phase** — `Evaluator` chains gauge constructors that register Micrometer metrics for each policy check.
3. **Serving phase** — `HttpServer` (NanoHTTPD) exposes `/metrics` (Prometheus format), `/liveness`, and `/readiness`.

`ScheduledEvaluation` coordinates the loop and stores the latest metric snapshot in an `AtomicReference` so the HTTP server can serve it independently.

### Key packages

| Package | Responsibility |
|---|---|
| `com.tastybug.vaultsnitch` | Entry point, HTTP server, scheduler, settings |
| `com.tastybug.vaultsnitch.collection` | Vault store/secret discovery |
| `com.tastybug.vaultsnitch.evaluation` | Gauge implementations and orchestration |

### Adding a new policy gauge

1. Create a class in `evaluation/` that accepts the collected data and a `PrometheusMeterRegistry`.
2. Register metrics using Micrometer's `Gauge.builder(...)`.
3. Wire the new gauge into `Evaluator`'s chain.
4. Expose any tuning knobs via environment variables read through `Settings`.

`TTLExpirationGauge` is a current skeleton intended as the next gauge implementation.

## Runtime Configuration

| Variable | Default | Purpose |
|---|---|---|
| `VAULT_URL` | `http://127.0.0.1:8200` | Vault server address |
| `VAULT_TOKEN` or `VAULT_TOKEN_FILE` | _(required)_ | Auth token or path to file containing it |
| `POLL_INTERVAL_SECS` | `300` | Evaluation loop interval |
| `SERVER_PORT` | `8080` | HTTP server port |
| `PasswordLengthGauge.Enabled` | `true` | Toggle complexity checks |
| `PasswordLengthGauge.Regex` | `^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z]).{22,}$` | Complexity pattern |

## Error Handling Convention

Failures in the collection or evaluation pipeline are represented as `Result` objects (carrying an exception flag) rather than propagating exceptions. The HTTP server continues serving the last-known-good metrics snapshot until a successful evaluation cycle replaces it.
