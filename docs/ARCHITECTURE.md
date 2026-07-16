# Architecture — Procurement SaaS Platform

A multi-tenant e-procurement / e-tender platform built as cloud-native microservices on
Java 21 + Spring Boot 3.4 + Spring Cloud, with a React/TypeScript front end.

## Design principles

1. **Bounded contexts over data tables** — each service owns its data; no shared database.
2. **API-first** — every service publishes an OpenAPI 3 contract.
3. **Event-driven where it fits** — long-running procurement flows coordinated with events + sagas, not synchronous call chains.
4. **Multi-tenant by design** — tenancy is resolved from the security token on every request.
5. **Stateless services** — horizontal scale; state in PostgreSQL / Redis / object storage.
6. **Secure by default** — zero-trust between services, least privilege, full audit.

## Topology

```
   React SPA ─┐
   Mobile ────┼──▶  API Gateway (Spring Cloud Gateway)  ◀──▶  Keycloak (OIDC)
   3rd-party ─┘            │  JWT (tenant, roles, features)
                           ▼
   ┌───────────┬───────────┬───────────┬───────────┬───────────┐
   │ Identity  │ Vendor    │ Tender    │ Evaluation│ Award &   │ ...
   │ & Access  │ Mgmt      │           │           │ Contract  │
   └───────────┴───────────┴───────────┴───────────┴───────────┘
        ▲                      Kafka (domain events)          ▲
        └───────────────────────────────────────────────────-┘
   Observability: OpenTelemetry → Prometheus / Grafana / Loki / Tempo
```

## Services

| Service | Responsibility |
|---------|----------------|
| Identity & Access | Authentication (Keycloak), users, roles, feature-level RBAC |
| Tenant & Billing | Tenant onboarding/provisioning, subscriptions, entitlements, metering |
| Vendor Management | Supplier profiles, contacts, bank accounts, documents, debarment |
| Master Data | Items, categories, units, currencies, geo, banks, code generation |
| Tender | Tender lifecycle, EOI, items/lots, committees, publishing, opening |
| Evaluation | Technical & financial evaluation, comparative statement, selection |
| Award & Contract | Award notices, work orders, delivery schedules, goods receipt |
| Workflow & Approval | Configurable approval workflows, delegation of authority |
| Enlistment / Pre-Qual | Supplier enlistment scheduling, criteria, participation |
| Notification | Email + in-app notifications, templating, delivery status |
| Reporting Engine | Async PDF/XLSX report generation (JasperReports / Apache POI) |
| Document Store | S3/MinIO-backed upload/download, versioning, signed URLs |

## Multi-tenancy

**Schema-per-tenant** on a shared PostgreSQL cluster:

- Strong logical isolation per tenant; per-tenant backup/restore.
- Implemented with Hibernate `MultiTenantConnectionProvider` +
  `CurrentTenantIdentifierResolver`. The tenant id is read from the `tenant` JWT claim,
  propagated by the gateway as `X-Tenant-ID`, and held in a request-scoped `TenantContext`.
- The Tenant & Billing service provisions a new schema (Flyway) and a Keycloak client at
  signup.

See the working implementation in
[`service-template/.../tenancy`](../service-template/src/main/java/com/procurementsaas/template/tenancy).

## Authentication & authorization

- OAuth2 / OpenID Connect via Keycloak.
- Short-lived JWTs carry `sub`, `tenant`, `roles`, and granted feature codes.
- Gateway validates the token and enforces route-level access; each service re-validates
  and enforces fine-grained, feature-level permissions with `@PreAuthorize`.

## Reporting

Preserve a JasperReports / Apache POI engine but run it as an independent, asynchronous
service: request → queue (Kafka) → worker pulls data via source-service read APIs →
render PDF/XLSX → store in object storage → return a signed download link. Recurring
reports are cron-driven with ShedLock.

## Data & consistency

- Database-per-service (logical); no cross-service joins — cross-context reads via APIs or
  replicated read models.
- Intra-service ACID transactions; cross-service **sagas** (orchestrated for the
  tender → evaluation → award flow) with compensating actions; **transactional outbox**
  for reliable event publishing.
- Flyway migrations per service, per tenant.

## Technology stack

Java 21 (virtual threads) · Spring Boot 3.4 · Spring Cloud 2024 · Spring Security
(OAuth2 Resource Server) · Spring Data JPA / Hibernate (multi-tenant) · Spring Batch ·
Spring StateMachine · PostgreSQL · Redis · Apache Kafka · Keycloak · S3/MinIO ·
JasperReports · Apache POI · Resilience4j · OpenTelemetry / Prometheus / Grafana / Loki /
Tempo · Docker · Kubernetes · React + TypeScript · Flyway · Testcontainers

## Roadmap

- [x] Platform foundation — API gateway + multi-tenant service template
- [x] `platform-common` — shared tenancy/security/error handling via auto-configuration
- [x] Identity & Access service — users, roles, feature-level RBAC, Keycloak JWT
- [x] Master Data service — units, currencies, items, geography (cached, seeded)
- [x] Vendor Management service — supplier lifecycle, contacts, documents, debarment
- [x] Tender service — lifecycle, items, participants, sealed bids, opening, award
- [x] Evaluation service — weighted technical + financial scoring, comparative statement
- [x] Event backbone (`platform-events`) — Kafka contracts, publish-after-commit
- [x] Notification service — event-driven, templated, idempotent
- [x] Tenant & Billing — onboarding with real schema provisioning, plans, quotas, invoices
- [x] Award & Contract — notice of award, work orders, delivery schedule, goods receipt
- [x] Workflow & Approval — configurable steps, separation of duties, delegation of authority
- [x] Enlistment / Pre-Qualification — rounds, mandatory criteria, time-bounded enlistment
- [x] Reporting engine — async jobs, pluggable renderers (XLSX/CSV) + data providers
- [x] React front end — Vite + TypeScript, OIDC-ready, feature-gated UI
