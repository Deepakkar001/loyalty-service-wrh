# tenant-onboarding-service

Part of the LoyaltyOS platform. Handles the 5-stage tenant onboarding workflow.

## Local Development Setup

### Prerequisites
- Java 21
- Docker Desktop

### Start infrastructure
```bash
docker-compose up -d
```

Wait ~30 seconds for all services to be healthy, then:

Note: this `docker-compose` is for Kafka/Redis. This service is designed to use **your locally installed MySQL 8**
on `localhost:3306`. Configure DB settings via environment variables (recommended):

- `DB_URL` (example: `jdbc:mysql://localhost:3306/loyaltyos_onboarding?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
- `DB_USERNAME`
- `DB_PASSWORD`

If you want local-only secrets (SMTP creds, JWT secret), create:

```bash
cp application-secrets.example.yml application-secrets.yml
```

(`application-secrets.yml` is intentionally ignored by git.)

### Run the application
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Swagger / API quickstart (Stage 1)

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

#### Register tenant (idempotent)

Include an `Idempotency-Key` header to make the request safe to retry (response is cached in Redis).

```bash
curl -X POST "http://localhost:8080/api/v1/onboarding/register" ^
  -H "Content-Type: application/json" ^
  -H "Idempotency-Key: 5cfd3e1f-7a0a-4a7d-9c5e-9f5a0f25f7c1" ^
  -d "{\"companyName\":\"Acme Corp\",\"email\":\"owner@acme.com\",\"countryCode\":\"IN\",\"identityMode\":\"BOTH\",\"subscriptionTier\":\"STARTER\",\"dataResidencyRegion\":\"AP_SOUTH_1\",\"primaryContactName\":\"Deepak\",\"primaryContactEmail\":\"deepak@example.com\"}"
```

#### Resend verification email

```bash
curl -X POST "http://localhost:8080/api/v1/onboarding/resend-verification" ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"owner@acme.com\"}"
```

#### Check onboarding status

```bash
curl "http://localhost:8080/api/v1/onboarding/{tenantId}/status"
```

### Build without tests
```bash
./gradlew build -x test
```

### Run tests
```bash
./gradlew test
```

### Services available locally
| Service    | URL                          |
|------------|------------------------------|
| App        | http://localhost:8080        |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Kafka UI   | http://localhost:8090        |
| MySQL      | localhost:3306               |
| Redis      | localhost:6379               |

