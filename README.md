# PRX — Sistema de Conciliação Financeira e Auditoria

Internal management tool for financial reconciliation. It audits card transaction fees charged by acquirers against contracted rates, generates AI-powered or template-based WhatsApp notifications, and tracks collection history per client.

---

## Architecture

```
prx-project/
└── fin-check/          ← Spring Boot REST API (this repo)
                           (separate microservice collector not included here)
```

The data collection microservice (external) hits the Conciflex API and populates the database. This API exposes the results for auditing and messaging.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Security | Spring Security + JWT (Bearer) |
| Persistence | Spring Data JPA + Hibernate + PostgreSQL |
| Migrations | Flyway |
| Build | Maven |
| Mapping | MapStruct + Lombok |
| HTTP clients | RestTemplate / WebClient |
| Messaging | Meta Cloud API (WhatsApp) |
| AI generation | Anthropic API (Claude) |
| Containers | Docker multi-stage (JDK 17 builder → JRE 17 slim) |

---

## Running Locally

### Prerequisites
- Docker & Docker Compose
- Java 17 (for local dev without Docker)

### With Docker Compose

```bash
cp .env.example .env
# fill in .env with real values
docker compose up --build
```

API available at `http://localhost:8080`.

### Without Docker

```bash
# Start only Postgres
docker compose up postgres -d

# Run the API
cd fin-check
./mvnw spring-boot:run
```

---

## Environment Variables

Copy `.env.example` to `.env` and fill in:

| Variable | Description |
|---|---|
| `JWT_SECRET` | HMAC-SHA256 signing key (min 32 chars) |
| `JWT_EXPIRATION_MS` | Token TTL in ms (default 8h = `28800000`) |
| `CRYPTO_SECRET_KEY` | AES-256 key for encrypting Conciflex credentials |
| `ANTHROPIC_API_KEY` | Claude API key for AI message generation |
| `META_ACCESS_TOKEN` | WhatsApp Cloud API access token |
| `META_PHONE_NUMBER_ID` | Meta phone number ID for sending messages |
| `META_WEBHOOK_SECRET` | Secret for validating Meta webhook signatures |

---

## API Reference

### Authentication
```
POST /api/auth/login
Body: { "login": "admin", "senha": "..." }
Returns: { "token": "...", "expiresIn": 28800000 }
```

All other endpoints require `Authorization: Bearer <token>`.

### Clients
```
GET    /api/clientes
GET    /api/clientes/{id}
POST   /api/clientes
PUT    /api/clientes/{id}
DELETE /api/clientes/{id}          — soft delete (ativo=false)
```

### Establishments
```
GET    /api/clientes/{clienteId}/estabelecimentos
POST   /api/clientes/{clienteId}/estabelecimentos
PUT    /api/estabelecimentos/{id}
DELETE /api/estabelecimentos/{id}  — soft delete
```

### Fee Audit
```
GET /api/auditoria/{estabelecimentoId}?dataInicio=YYYY-MM-DD&dataFim=YYYY-MM-DD
```
Returns total transactions, amount overcharged, amount undercharged, and a breakdown by card brand.

### Receivables
```
GET /api/recebimentos/{estabelecimentoId}?dataInicio=YYYY-MM-DD&dataFim=YYYY-MM-DD
```
Returns total received, total discounted in fees, breakdown by card brand.

### Messages
```
POST /api/mensagens/gerar     — generate (AI or template), does not send yet
POST /api/mensagens/enviar    — send via WhatsApp and save to history
GET  /api/mensagens/{clienteId}
```

`modo` field: `"ia"` uses Claude to generate a personalized message; `"template"` fills a fixed template from `application.yml`.

### Webhook (Meta)
```
GET  /api/webhook/meta   — verification challenge
POST /api/webhook/meta   — delivery status callbacks (validated via X-Hub-Signature-256)
```

### Collection Logs
```
GET /api/logs/coleta?estabelecimentoId=&status=
```

---

## Database Schema

Managed by Flyway migrations in `src/main/resources/db/migration/`:

| Migration | Table |
|---|---|
| V1 | `clientes` |
| V2 | `estabelecimentos` |
| V3 | `conciliacao_taxas` — fee records from Conciflex API |
| V4 | `recebimentos` — receivable records (payments + debit adjustments) |
| V5 | `mensagens_enviadas` |
| V6 | `logs_coleta` |
| V7 | `users` — seeded admin user |

> **Note:** Conciflex credentials (`conciflex_login`, `conciflex_senha`) are always stored AES-256-GCM encrypted.

---

## Security

- All routes are JWT-protected except `POST /api/auth/login` and `POST /api/webhook/meta`
- Single admin user, seeded via Flyway with a BCrypt-hashed password
- Meta webhook payloads are validated with HMAC-SHA256 against `META_WEBHOOK_SECRET`

---

## Error Responses

All errors follow the same structure:

```json
{
  "timestamp": "2026-04-19T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Cliente não encontrado",
  "path": "/api/clientes/abc"
}
```

| HTTP Status | Cause |
|---|---|
| 400 | Validation error / bad input |
| 401 | Missing or invalid JWT |
| 404 | Resource not found |
| 502 | Anthropic or Meta API failure |
| 500 | Unexpected server error |

---

## Default Credentials

Seeded by `V7__seed_admin_user.sql`:

- **Login:** `admin`
- **Password:** `admin123` (change before any real deployment)
