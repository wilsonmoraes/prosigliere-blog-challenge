# Blog Platform API

A small RESTful API for managing blog posts and their comments. Built with **Spring Boot 3.3**, **Java 21** and **Maven**, persisted in an embedded **H2** database so it runs with zero setup.

## Tech stack

- Java 21
- Spring Boot 3.3 (Web, Data JPA, Validation)
- H2 in-memory database (PostgreSQL compatibility mode)
- springdoc-openapi (Swagger UI)
- Lombok
- JUnit 5, Mockito, AssertJ, Spring MockMvc

## Project layout

```
src/main/java/com/wilsonmoraes/blogplatform
├── BlogPlatformApiApplication.java
├── domain/          # JPA entities (BlogPost, Comment)
├── repository/      # Spring Data repositories + projections
├── service/         # business logic (transaction boundaries)
└── web/
    ├── BlogPostController, CommentController
    ├── dto/         # request/response records with Bean Validation
    └── exception/   # GlobalExceptionHandler using RFC 7807 ProblemDetail
```

Layered architecture: HTTP DTOs never leak below the `web` package, and JPA entities never leak above the `service` package — controllers map to/from DTOs.

## Requirements

- JDK 21 on your PATH (`java -version` should print `21.x`)
- That's it. The Maven Wrapper (`./mvnw`) downloads everything else.

## Run it

```bash
# Linux/macOS
./mvnw spring-boot:run

# Windows PowerShell
.\mvnw.cmd spring-boot:run
```

The API starts on **http://localhost:8080**.

Useful URLs:

| URL | What it is |
| --- | --- |
| `http://localhost:8080/swagger-ui.html` | Interactive API docs (Swagger UI) |
| `http://localhost:8080/v3/api-docs`     | OpenAPI 3 JSON spec |
| `http://localhost:8080/h2-console`      | H2 web console (JDBC URL: `jdbc:h2:mem:blogdb`, user: `sa`, no password) |

## Run the tests

```bash
./mvnw test
```

Test coverage includes:

- `BlogPostServiceTest` — unit tests for the service layer (Mockito).
- `BlogPostControllerTest` — `@WebMvcTest` slice covering happy paths, validation errors, and 404s for all four endpoints.
- `BlogPostRepositoryTest` — `@DataJpaTest` that asserts the JPQL summary query returns the correct comment counts.
- `BlogPlatformApiApplicationTests` — Spring context smoke test.

## Endpoints

All endpoints live under `/api/posts`.

### `GET /api/posts`

Lists blog posts with their title and comment count. Supports paging via `?page=` and `?size=` (default `size=20`).

```bash
curl http://localhost:8080/api/posts
```

```json
{
  "content": [
    { "id": 1, "title": "Hello world", "commentCount": 2, "createdAt": "2026-05-27T12:00:00Z" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### `POST /api/posts`

Creates a new blog post. Returns `201 Created` with a `Location` header.

```bash
curl -X POST http://localhost:8080/api/posts \
  -H 'Content-Type: application/json' \
  -d '{"title":"Hello world","content":"My first post"}'
```

### `GET /api/posts/{id}`

Retrieves a single post with its full content and the list of comments. Returns `404 Not Found` (as RFC 7807 ProblemDetail) when the id does not exist.

```bash
curl http://localhost:8080/api/posts/1
```

```json
{
  "id": 1,
  "title": "Hello world",
  "content": "My first post",
  "createdAt": "2026-05-27T12:00:00Z",
  "updatedAt": "2026-05-27T12:00:00Z",
  "comments": [
    { "id": 1, "author": "alice", "content": "nice!", "createdAt": "2026-05-27T12:01:00Z" }
  ]
}
```

### `POST /api/posts/{id}/comments`

Adds a comment to an existing post.

```bash
curl -X POST http://localhost:8080/api/posts/1/comments \
  -H 'Content-Type: application/json' \
  -d '{"author":"alice","content":"nice!"}'
```

## Error responses

Errors follow [RFC 7807 (`application/problem+json`)](https://datatracker.ietf.org/doc/html/rfc7807):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "errors": [
    { "field": "title", "message": "must not be blank" }
  ]
}
```

## Design notes

- **DTOs as Java records** keep the wire contract immutable and explicit, and they're decoupled from entities.
- **`@OneToMany` with `orphanRemoval`** on `BlogPost.comments` means a deleted post takes its comments with it.
- **Comment count via JPQL subquery** instead of loading every comment — avoids N+1 on the list endpoint.
- **`open-in-view: false`** to fail fast on accidental lazy loading outside a transaction.
- **Auditing fields** (`createdAt`, `updatedAt`) populated automatically via `@EnableJpaAuditing`.
- **Optimistic locking** via `@Version` on `BlogPost`.
- **Global exception handler** returns `ProblemDetail` for not-found, validation, malformed JSON, and type mismatch.

## Next steps if I had more time

- **Persistence**: swap H2 for PostgreSQL via Docker Compose, manage schema with **Flyway**, and use **Testcontainers** for integration tests against a real Postgres.
- **Update / delete endpoints** for posts and comments (`PUT /api/posts/{id}`, `DELETE /api/posts/{id}`, `DELETE /api/posts/{id}/comments/{commentId}`), with proper authorization on who can edit/delete.
- **Authentication** with Spring Security (JWT or session) so comments and posts have an authenticated author instead of a free-form `author` string.
- **Cursor or keyset pagination** on `GET /api/posts` once the dataset is large — offset paging gets expensive past a few hundred thousand rows.
- **Dedicated `CommentRepository`** with a paginated `GET /api/posts/{id}/comments` endpoint, since loading every comment on the post detail view doesn't scale.
- **Caching** of the list endpoint with Spring Cache + Caffeine, invalidated on writes.
- **Observability**: Spring Boot Actuator + Micrometer with Prometheus, structured JSON logs, request tracing via `traceparent`.
- **CI**: GitHub Actions workflow running `./mvnw verify` on every PR, plus JaCoCo for coverage and Spotless/Checkstyle for formatting.
- **Container image**: `Dockerfile` (or `spring-boot:build-image`) so the API can be deployed anywhere a JVM is not pre-installed.
- **Rate limiting** on the public write endpoints (e.g. Bucket4j) to keep abusive clients in check.
