# Open Finance Payment Processor

A robust, event-driven payment processing system for Brazilian Open Finance, featuring async processing, automatic reconciliation, and complete auditability.

## 📋 Table of Contents
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Monitoring](#monitoring)
- [Development](#development)

## ✨ Features

- **Async Payment Processing**: Queue-based processing with retry logic
- **Idempotency**: Prevent duplicate payments with idempotency keys
- **Auto Reconciliation**: Automatic status verification with banks
- **Complete Audit Trail**: Immutable event log for all transactions
- **Brazilian Finance Support**: CPF/CNPJ validation, PIX key validation
- **High Availability**: Resilient design with retry and circuit breaker patterns
- **Observability**: Structured JSON logs, Prometheus metrics
- **Docker Support**: Complete containerized setup with Docker Compose

## 🏗️ Architecture

### Microservices

```
┌─────────────────┐     ┌─────────────────────┐     ┌──────────────────────┐
│  Payment API    │────▶│ Processing Service  │────▶│ Reconciliation Svc   │
│  (Port 8082)    │     │   (Port 8083)       │     │    (Port 8084)       │
│                 │     │                     │     │                      │
│ - Create payment│     │ - Process queue     │     │ - Verify status      │
│ - Query payment │     │ - Call bank APIs    │     │ - Fix inconsistencies│
│ - List payments │     │ - Retry failures    │     │ - Auto reconcile     │
└─────────────────┘     └─────────────────────┘     └──────────────────────┘
         │                       │                             │
         └───────────────────────┴─────────────────────────────┘
                                 │
                    ┌────────────┴─────────────┐
                    │                          │
              ┌─────▼──────┐          ┌───────▼────┐
              │ PostgreSQL │          │   Redis    │
              │  Database  │          │   Cache    │
              └────────────┘          └────────────┘
```

### Database Schema

- **payments**: Stores all payment transactions
- **payment_queue**: Async processing queue with retry logic
- **audit_log**: Immutable audit trail of all events

## 🛠️ Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0
  - Spring Web (REST APIs)
  - Spring Data JPA
  - Spring Scheduling (Background jobs)
  - Spring Security (JWT)
  - Spring Actuator (Health checks)
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Metrics**: Micrometer + Prometheus
- **Logging**: Logback with JSON encoder
- **Testing**: JUnit 5, Mockito, TestContainers
- **Build**: Maven
- **Container**: Docker + Docker Compose

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- (Optional) PostgreSQL 15 & Redis 7 for local development

### Quick Start with Docker Compose

1. **Clone the repository**
```bash
git clone <repository-url>
cd marcus-payment-processor
```

2. **Build and start all services**
```bash
docker-compose up --build
```

This will start:
- PostgreSQL (port 5433)
- Redis (port 6380)
- Payment API (port 8082)
- Processing Service (port 8083)
- Reconciliation Service (port 8084)

3. **Wait for services to be healthy**
```bash
# Check service health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

### Local Development (without Docker)

1. **Start PostgreSQL and Redis**
```bash
# PostgreSQL
docker run -d \
  --name payment-postgres \
  -e POSTGRES_DB=payment_processor \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin123 \
  -p 5433:5432 \
  postgres:15-alpine

# Redis
docker run -d \
  --name payment-redis \
  -p 6380:6379 \
  redis:7-alpine
```

2. **Initialize database schema**
```bash
psql -h localhost -p 5433 -U admin -d payment_processor -f database/schema.sql
```

3. **Build the project**
```bash
mvn clean install
```

4. **Run each service in separate terminals**
```bash
# Terminal 1 - Payment API
cd payment-api
mvn spring-boot:run

# Terminal 2 - Processing Service
cd processing-service
mvn spring-boot:run

# Terminal 3 - Reconciliation Service
cd reconciliation-service
mvn spring-boot:run
```

## 📖 API Documentation

### Authentication

All API endpoints require HTTP Basic Authentication:
- Username: `user`
- Password: `password`

### Create Payment

```bash
POST /api/v1/payments
Headers:
  Content-Type: application/json
  Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
  Authorization: Basic dXNlcjpwYXNzd29yZA==

Body:
{
  "type": "PIX",
  "amount": "150.00",
  "currency": "BRL",
  "sender": {
    "document": "12345678909",
    "bankCode": "001",
    "account": "12345-6"
  },
  "receiver": {
    "pixKey": "user@example.com",
    "pixKeyType": "EMAIL"
  }
}

Response 201 Created:
{
  "paymentId": "PAY-2025-000001",
  "status": "PENDING",
  "amount": "150.00",
  "currency": "BRL",
  "createdAt": "2025-11-19T19:00:00Z",
  "estimatedCompletion": "2025-11-19T19:00:30Z"
}
```

### Get Payment

```bash
GET /api/v1/payments/{paymentId}
Authorization: Basic dXNlcjpwYXNzd29yZA==

Response 200 OK:
{
  "paymentId": "PAY-2025-000001",
  "status": "SUCCESS",
  "amount": "150.00",
  "currency": "BRL",
  "confirmationCode": "E12345678202511191900001234567890",
  "createdAt": "2025-11-19T19:00:00Z",
  "processedAt": "2025-11-19T19:00:15Z",
  "timeline": [
    {
      "status": "PENDING",
      "timestamp": "2025-11-19T19:00:00Z"
    },
    {
      "status": "PROCESSING",
      "timestamp": "2025-11-19T19:00:05Z"
    },
    {
      "status": "SUCCESS",
      "timestamp": "2025-11-19T19:00:15Z"
    }
  ]
}
```

### List Payments

```bash
GET /api/v1/payments?status=SUCCESS&page=0&size=20&sort=createdAt,desc
Authorization: Basic dXNlcjpwYXNzd29yZA==

Response 200 OK:
{
  "content": [
    {
      "paymentId": "PAY-2025-000002",
      "status": "SUCCESS",
      "amount": "200.00",
      "createdAt": "2025-11-19T19:05:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

### Example cURL Commands

```bash
# Create payment
curl -X POST http://localhost:8082/api/v1/payments \
  -u user:password \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "type": "PIX",
    "amount": "150.00",
    "currency": "BRL",
    "sender": {
      "document": "12345678909",
      "bankCode": "001",
      "account": "12345-6"
    },
    "receiver": {
      "pixKey": "user@example.com",
      "pixKeyType": "EMAIL"
    }
  }'

# Get payment
curl http://localhost:8082/api/v1/payments/PAY-2025-000001 \
  -u user:password

# List payments
curl "http://localhost:8082/api/v1/payments?page=0&size=10" \
  -u user:password
```

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

### Run Tests with Coverage

```bash
mvn clean test jacoco:report
```

View coverage report: `target/site/jacoco/index.html`

### Test Structure

- **Unit Tests** (~85 tests): Validators, services, DTOs
- **Integration Tests** (~40 tests): Controllers, repositories with TestContainers
- **E2E Tests** (~15 tests): Full payment flow

### Sample Test Commands

```bash
# Run only unit tests
mvn test -Dtest="*Test"

# Run only integration tests
mvn test -Dtest="*IntegrationTest"

# Run specific test class
mvn test -Dtest=CPFValidatorTest
```

## 📊 Monitoring

### Health Checks

```bash
# Payment API
curl http://localhost:8082/actuator/health

# Processing Service
curl http://localhost:8083/actuator/health

# Reconciliation Service
curl http://localhost:8084/actuator/health
```

### Metrics (Prometheus)

```bash
# View all metrics
curl http://localhost:8082/actuator/prometheus
```

Key metrics:
- `payments_created_total`: Total payments created
- `payments_success_total`: Successfully processed payments
- `payments_failed_total`: Failed payments
- `payments_queue_size`: Current queue size
- `payments_processing_duration_seconds`: Processing time distribution
- `payments_reconciled_total`: Reconciled payments
- `payments_inconsistencies_total`: Detected inconsistencies

### Logs

All services output structured JSON logs:

```json
{
  "timestamp": "2025-11-19T19:00:00.000Z",
  "level": "INFO",
  "service": "payment-api",
  "logger": "com.openfinance.payment.api.controller.PaymentController",
  "message": "Payment created",
  "payment_id": "PAY-2025-000001",
  "amount": "150.00"
}
```

## 💻 Development

### Project Structure

```
marcus-payment-processor/
├── common/                      # Shared entities, DTOs, validators
│   ├── entity/                  # JPA entities
│   ├── dto/                     # Request/Response DTOs
│   ├── repository/              # Spring Data repositories
│   ├── validator/               # CPF, CNPJ, PIX validators
│   └── util/                    # Utility classes
├── payment-api/                 # REST API service
│   ├── controller/              # REST controllers
│   ├── service/                 # Business logic
│   ├── exception/               # Exception handlers
│   └── config/                  # Spring configuration
├── processing-service/          # Async processing service
│   ├── service/                 # Processing logic
│   ├── scheduler/               # Scheduled tasks
│   └── client/                  # Bank API clients (mock)
├── reconciliation-service/      # Reconciliation service
│   ├── service/                 # Reconciliation logic
│   ├── scheduler/               # Scheduled tasks
│   └── client/                  # Bank API clients
├── database/                    # Database scripts
│   ├── schema.sql               # DDL
│   └── seed.sql                 # Sample data
├── docker-compose.yml           # Docker Compose config
└── pom.xml                      # Parent POM
```

### Adding New Features

1. **Add entities** in `common/entity/`
2. **Add DTOs** in `common/dto/`
3. **Add repositories** in `common/repository/`
4. **Implement service** in respective microservice
5. **Add tests** (unit + integration)
6. **Update API documentation** in README

### Code Style

- Follow standard Java naming conventions
- Use Lombok for boilerplate code reduction
- Write tests for all business logic
- Add JavaDoc for public APIs
- Use meaningful commit messages

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | - | Active Spring profile (docker, prod) |
| `SPRING_DATASOURCE_URL` | jdbc:postgresql://localhost:5433/payment_processor | Database URL |
| `SPRING_DATASOURCE_USERNAME` | admin | Database username |
| `SPRING_DATASOURCE_PASSWORD` | admin123 | Database password |
| `SPRING_DATA_REDIS_HOST` | localhost | Redis host |
| `SPRING_DATA_REDIS_PORT` | 6380 | Redis port |

## 🔒 Security

- **Authentication**: HTTP Basic Auth (replace with JWT in production)
- **Input Validation**: Hibernate Validator + custom validators
- **SQL Injection**: Protected by JPA/Hibernate
- **Sensitive Data**: CPF/CNPJ masked in logs
- **LGPD Compliance**: Personal data encryption (to be implemented)

## 📝 License

This project is for educational/portfolio purposes.

## 🤝 Contributing

This is a portfolio project, but suggestions are welcome! Please open an issue to discuss changes.

## 📧 Contact

For questions or feedback, please open an issue in the repository.

---

**Note**: This is a demonstration project with mock bank integrations. In production, replace mock clients with real banking APIs and implement proper security measures (JWT, encryption, etc.).