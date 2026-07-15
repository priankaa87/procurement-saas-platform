# Procurement SaaS Platform

A multi-tenant **e-procurement / e-tender** platform built as a cloud-native
microservices system on **Java 21 + Spring Boot 3.4 + Spring Cloud**, with a
React/TypeScript front end.

> Personal project. Architecture and code are my own, written from scratch to explore
> modern microservices, multi-tenant SaaS, and event-driven design for a procurement
> domain.

## Highlights

- **Microservices** behind a Spring Cloud Gateway (edge auth, routing, rate limiting)
- **Multi-tenant SaaS** — schema-per-tenant isolation, tenant resolved from the JWT
- **OAuth2 / OIDC** authentication via Keycloak; fine-grained, feature-level authorization
- **Event-driven** coordination (Kafka) with sagas for long-running procurement flows
- **Async report engine** (JasperReports / Apache POI) for tender & evaluation reports
- **Observability** built in (OpenTelemetry, Prometheus, Grafana, Loki, Tempo)

## Planned services

| Service | Responsibility |
|---------|----------------|
| Identity & Access | Users, roles, feature-level RBAC |
| Tenant & Billing | Onboarding, subscriptions, entitlements |
| Vendor Management | Supplier profiles, documents, debarment |
| Master Data | Items, categories, units, currencies, geo |
| Tender | Tender lifecycle, EOI, items/lots, bidding |
| Evaluation | Technical & financial evaluation, comparative statement |
| Award & Contract | Award notices, work orders, delivery schedules |
| Workflow & Approval | Configurable approval workflows, delegation |
| Notification | Email + in-app notifications, templating |
| Reporting Engine | Async PDF/XLSX report generation |
| Document Store | S3/MinIO-backed file storage |

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full design.

## Repository layout

```
procurement-saas-platform/
├── platform-common/            # Shared: multi-tenancy, security, error handling (auto-config)
├── api-gateway/                # Spring Cloud Gateway (edge, auth, routing)
├── service-template/           # Copy-me archetype for every business service
├── identity-service/           # Users, roles, feature-level RBAC, Keycloak JWT
├── master-data-service/        # Units, currencies, items, geography (cached)
├── vendor-management-service/  # Supplier lifecycle, contacts, documents, debarment
├── docs/                       # Architecture & design notes
└── docker-compose.yml          # Local infra: PostgreSQL, Keycloak, Redis, MinIO
```

Cross-cutting concerns live once in `platform-common` and are applied to every service via
Spring Boot auto-configuration — a service gets multi-tenancy and JWT security by adding a
single dependency, and can override any bean.

## Quick start

```bash
# 1. Start local infrastructure
docker compose up -d

# 2. Create a Keycloak realm named "procurement" (http://localhost:8081)

# 3. Generate the Gradle wrapper, then build & run
gradle wrapper
./gradlew build
./gradlew :service-template:bootRun    # http://localhost:8090/swagger-ui.html
./gradlew :api-gateway:bootRun         # http://localhost:8080
```

## Tech stack

Java 21 · Spring Boot 3.4 · Spring Cloud 2024 · Spring Security (OAuth2 Resource Server) ·
Spring Data JPA / Hibernate (multi-tenant) · PostgreSQL · Redis · Kafka · Keycloak ·
Flyway · JasperReports · Apache POI · Docker · Kubernetes · React + TypeScript

## Status

🚧 Early scaffold — the platform foundation (gateway + multi-tenant service template) is
in place; business services are being added incrementally.

## License

[MIT](LICENSE)
