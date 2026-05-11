# Ecommerce Backend

A Spring Boot REST API for ecommerce backend services. The project currently includes category management endpoints, PostgreSQL persistence, Flyway database migrations, OpenAPI documentation, and a Postman collection for API testing.

## Tech Stack

- Java 25
- Spring Boot 4.0.6
- Gradle
- PostgreSQL 17.5
- Spring Data JPA
- Flyway
- Springdoc OpenAPI / Swagger UI
- MapStruct
- Lombok

## Prerequisites

Install the following tools before running the application:

- JDK 25
- Docker Desktop or Docker Engine with Docker Compose
- Postman

The project includes the Gradle wrapper, so a separate Gradle installation is not required.

## Database Setup with Docker Compose

The application uses PostgreSQL. A ready-to-use Docker Compose configuration is available in `compose.yml`.

Start PostgreSQL:

```bash
docker compose up -d
```

Verify that the container is running:

```bash
docker compose ps
```

Default database configuration:

| Property | Value |
| --- | --- |
| Container name | `ecommerce_postgres` |
| Host | `localhost` |
| Port | `5432` |
| Database | `ecommerce_db` |
| Username | `admin` |
| Password | `admin@123` |

Stop PostgreSQL:

```bash
docker compose stop
```

Stop PostgreSQL and remove the persisted database volume:

```bash
docker compose down -v
```

Use `down -v` only when you want to delete local database data and start fresh.

## Configuration

The default active Spring profile is `dev`.

Main configuration files:

- `src/main/resources/application.yaml`
- `src/main/resources/application-dev.yaml`

The development profile connects to:

```text
jdbc:postgresql://localhost:5432/ecommerce_db?createDatabaseIfNotExist=true
```

Flyway migrations are stored in:

```text
src/main/resources/db/migration
```

When the application starts, Flyway applies the database migrations automatically.

## Running the Application

Start PostgreSQL first:

```bash
docker compose up -d
```

Run the Spring Boot application:

```bash
./gradlew bootRun
```

On Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

The application starts on:

```text
http://localhost:8080
```

## API Documentation

After the application is running, open Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

Current base API path:

```text
/api/v1
```

Category endpoints are available under:

```text
/api/v1/categories
```

## Using the Postman Collection

A Postman collection is included in the repository:

```text
Ecommerce Backend.postman_collection.json
```

To use it:

1. Open Postman.
2. Select **Import**.
3. Choose `Ecommerce Backend.postman_collection.json`.
4. Confirm that the collection variable `BASE_URL` is set to:

```text
http://localhost:8080
```

5. Start PostgreSQL and the Spring Boot application.
6. Run the requests from the collection.

The collection includes:

- Happy path category CRUD requests
- Duplicate category validation
- Not found scenarios
- Request validation scenarios
- Category status activation and deactivation

Recommended execution order:

1. `Create Category`
2. `List Categories`
3. `Create Category - Duplicate Code`
4. `Get Category By ID`
5. `Update Category`
6. `Deactivate Category`
7. `Activate Category`
8. `Delete Category`

The collection uses variables such as `CATEGORY_ID`, `CATEGORY_CODE`, and `BASE_URL` to pass values between requests.

## Running Tests

Run the test suite with:

```bash
./gradlew test
```

On Windows PowerShell:

```powershell
.\gradlew.bat test
```

## Building the Project

Create a build artifact:

```bash
./gradlew build
```

On Windows PowerShell:

```powershell
.\gradlew.bat build
```

The generated artifact is created under:

```text
build/libs
```

## Useful Commands

```bash
# Start PostgreSQL
docker compose up -d

# View PostgreSQL logs
docker compose logs -f db

# Stop PostgreSQL
docker compose down

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Build the application
./gradlew build
```

## Troubleshooting

### Port 5432 is already in use

Another PostgreSQL instance may already be running on your machine. Stop the local instance or change the host port in `compose.yml`.

### Application cannot connect to PostgreSQL

Check that Docker is running and the database container is healthy:

```bash
docker compose ps
docker compose logs -f db
```

Also verify that the username, password, database name, and port match `application-dev.yaml`.

### Flyway migration errors

If local development data is no longer needed, reset the database volume:

```bash
docker compose down -v
docker compose up -d
```

Then start the application again so Flyway can re-apply migrations.
