# Patient Management Microservice

A Spring Boot microservices project for managing patients, authentication, billing account creation, and patient analytics events.

The project demonstrates a practical microservice architecture using REST APIs, Spring Cloud Gateway, JWT authentication, gRPC, Kafka, Protobuf, Spring Data JPA, and Maven.

## Architecture

```text
Client
  |
  v
API Gateway :4004
  |---------------------> Auth Service :4005
  |
  | JWT-protected route
  v
Patient Service :4000
  |---------------------> Billing Service gRPC :9002
  |
  | Kafka patient event
  v
Analytics Service
```

## Services

| Service | Port | Responsibility |
| --- | ---: | --- |
| API Gateway | `4004` | Single entry point, routes requests, validates JWT for protected patient APIs |
| Auth Service | `4005` | User login, BCrypt password verification, JWT generation and validation |
| Patient Service | `4000` | Patient CRUD, validation, persistence, billing integration, Kafka event publishing |
| Billing Service | `4001`, gRPC `9002` | Internal billing account creation API over gRPC |
| Analytics Service | default Spring port | Consumes patient events from Kafka |

## Tech Stack

- Java 21
- Spring Boot
- Spring Cloud Gateway
- Spring Security
- Spring Data JPA
- PostgreSQL / H2 support depending on service configuration
- gRPC
- Protocol Buffers
- Apache Kafka
- Maven
- RestAssured for integration tests
- Dockerfiles for each service

## Main Flows

### Authentication

1. Client calls `POST /auth/login` through the API Gateway.
2. Gateway forwards the request to Auth Service.
3. Auth Service verifies the user password using BCrypt.
4. Auth Service returns a signed JWT.
5. Client sends the token as:

```http
Authorization: Bearer <token>
```

### Create Patient

1. Client calls `POST /api/patients` with a valid JWT.
2. API Gateway validates the JWT by calling Auth Service.
3. Gateway forwards the request to Patient Service.
4. Patient Service validates the request and checks duplicate email.
5. Patient Service saves the patient.
6. Patient Service calls Billing Service over gRPC.
7. Patient Service publishes a patient-created event to Kafka.
8. Analytics Service consumes the event asynchronously.

## Repository Structure

```text
patient-management/
  api-gateway/
  auth-service/
  patient-service/
  billing-service/
  analytics-service/
  integration-tests/
  INTERVIEW_PREP.md
  pom.xml
```

## Prerequisites

Install these before running the project:

- Java 21
- Maven 3.9+
- Docker, optional but useful for databases/Kafka
- PostgreSQL, if running services with Postgres
- Kafka, if testing event publishing and analytics
- An API client such as Postman, Insomnia, curl, or IntelliJ HTTP Client

## Local Setup

Clone the repository:

```bash
git clone <your-repository-url>
cd PatientManagement_Microservice/patient-management
```

Build all Maven modules:

```bash
mvn clean package
```

If you want to skip tests during the first build:

```bash
mvn clean package -DskipTests
```

## Required Runtime Configuration

Some services need runtime properties. You can pass them as environment variables or command-line properties.

### API Gateway

The gateway needs the Auth Service URL:

```bash
--auth.service.url=http://localhost:4005
```

### Patient Service

The patient service needs to know where Billing Service is running:

```bash
--billing.service.address=localhost
--billing.service.grpc.port=9002
```

If Kafka is running somewhere other than `localhost:9092`, configure:

```bash
--spring.kafka.bootstrap-servers=localhost:9092
```

If using PostgreSQL, configure datasource properties:

```bash
--spring.datasource.url=jdbc:postgresql://localhost:5432/patient_db
--spring.datasource.username=postgres
--spring.datasource.password=postgres
--spring.jpa.hibernate.ddl-auto=update
--spring.sql.init.mode=always
```

### Auth Service

Auth Service already contains a sample JWT secret in `application.properties`. For real projects, pass the secret from an environment variable or secret manager.

Example database configuration:

```bash
--spring.datasource.url=jdbc:h2:mem:authdb
--spring.datasource.driver-class-name=org.h2.Driver
--spring.datasource.username=sa
--spring.datasource.password=
--spring.jpa.hibernate.ddl-auto=update
--spring.sql.init.mode=always
```

## Run Locally

Open separate terminals and start the services in this order.

### 1. Start Auth Service

```bash
cd auth-service
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:mem:authdb --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=update --spring.sql.init.mode=always"
```

### 2. Start Billing Service

```bash
cd billing-service
mvn spring-boot:run
```

Billing Service uses:

- HTTP: `4001`
- gRPC: `9002`

### 3. Start Patient Service

For H2-based local development, make sure H2 is available in the service dependencies or configure PostgreSQL.

PostgreSQL example:

```bash
cd patient-service
mvn spring-boot:run -Dspring-boot.run.arguments="--billing.service.address=localhost --billing.service.grpc.port=9002 --spring.datasource.url=jdbc:postgresql://localhost:5432/patient_db --spring.datasource.username=postgres --spring.datasource.password=postgres --spring.jpa.hibernate.ddl-auto=update --spring.sql.init.mode=always --spring.kafka.bootstrap-servers=localhost:9092"
```

### 4. Start Analytics Service

Start this after Kafka is running:

```bash
cd analytics-service
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.kafka.bootstrap-servers=localhost:9092"
```

### 5. Start API Gateway

```bash
cd api-gateway
mvn spring-boot:run -Dspring-boot.run.arguments="--auth.service.url=http://localhost:4005"
```

The application entry point is now:

```text
http://localhost:4004
```

## API Usage

### Login

Seed credentials from `auth-service/src/main/resources/data.sql`:

```text
email: testuser@test.com
password: password123
```

Request:

```bash
curl -X POST http://localhost:4004/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@test.com",
    "password": "password123"
  }'
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Get Patients

```bash
curl http://localhost:4004/api/patients \
  -H "Authorization: Bearer <token>"
```

### Create Patient

```bash
curl -X POST http://localhost:4004/api/patients \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ravi Kumar",
    "email": "ravi.kumar@example.com",
    "address": "Bhubaneswar, Odisha",
    "dateOfBirth": "1998-05-10",
    "registeredDate": "2026-06-08"
  }'
```

### Update Patient

```bash
curl -X PUT http://localhost:4004/api/patients/<patient-id> \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ravi Kumar Updated",
    "email": "ravi.updated@example.com",
    "address": "Cuttack, Odisha",
    "dateOfBirth": "1998-05-10",
    "registeredDate": "2026-06-08"
  }'
```

### Delete Patient

```bash
curl -X DELETE http://localhost:4004/api/patients/<patient-id> \
  -H "Authorization: Bearer <token>"
```

## Integration Tests

The integration tests expect the services to be running through the API Gateway at:

```text
http://localhost:4004
```

Run tests:

```bash
cd integration-tests
mvn test
```

## Docker

Each service contains its own Dockerfile. Example:

```bash
cd auth-service
docker build -t patient-auth-service .
docker run -p 4005:4005 patient-auth-service
```

Repeat for other services as needed.

There is currently no `docker-compose.yml` in the repository. To run the full system with Docker, add a Compose file for:

- API Gateway
- Auth Service
- Patient Service
- Billing Service
- Analytics Service
- PostgreSQL
- Kafka
- Zookeeper or Kafka KRaft setup

## Known Configuration Notes

- `api-gateway` requires `auth.service.url`.
- `billing-service` is configured with gRPC port `9002`, while its Dockerfile exposes `9001`; align these before Docker-based deployment.
- `patient-service` should be run with `billing.service.grpc.port=9002`.
- `patient-service` currently publishes Kafka events to topic `PATIENT_CREATED`.
- `analytics-service` currently listens to topic `patient`; align the topic names before relying on analytics events.
- In production, do not keep JWT secrets in `application.properties`.




