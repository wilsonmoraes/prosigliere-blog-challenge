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
├── config/          # JpaAuditingConfig (kept separate so @WebMvcTest doesn't drag JPA in)
├── domain/          # JPA entities (BlogPost, Comment)
├── repository/      # Spring Data repositories + JPQL summary projection
├── service/         # business logic (transaction boundaries)
└── web/
    ├── BlogPostController, CommentController
    ├── dto/         # request/response records with Bean Validation
    └── exception/   # GlobalExceptionHandler using RFC 7807 ProblemDetail
```

Layered architecture: HTTP DTOs never leak below the `web` package, and JPA entities never leak above the `service` package — controllers map to/from DTOs.

`BlogPost` deliberately **does not** map an inverse `@OneToMany List<Comment>` collection. Comments are reached only through `CommentRepository` so the post detail endpoint can paginate / cap the comment list and we avoid the usual `LazyInitializationException`, N+1, and cascade surprises.

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

## Try it in Postman

A ready-to-import collection lives at [`postman_collection.json`](./postman_collection.json) at the root of this repo. Open Postman → **Import** → drop the file. It exposes:

- A `baseUrl` collection variable defaulting to `http://localhost:8080`.
- A `postId` collection variable that the **Create blog post** request auto-fills via a test script, so the comment requests just work after you run *Create* once.

## Run the tests

```bash
./mvnw test
```

Test coverage includes:

- `BlogPostServiceTest` — unit tests for the service layer (Mockito).
- `BlogPostControllerTest` — `@WebMvcTest` slice covering happy paths, validation errors, and 404s for every endpoint.
- `BlogPostRepositoryTest` — `@DataJpaTest` validating the JPQL summary query (comment count via subquery).
- `CommentRepositoryTest` — `@DataJpaTest` for paginated lookup by `blogPostId`.
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

Retrieves a single post with its content and the **20 oldest comments**. Returns `404 Not Found` (as RFC 7807 ProblemDetail) when the id does not exist. For the full, paginated comment list use `GET /api/posts/{id}/comments`.

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

### `GET /api/posts/{id}/comments`

Paginated list of comments for a post (oldest first by default). Supports `?page=`, `?size=`, `?sort=`.

```bash
curl 'http://localhost:8080/api/posts/1/comments?page=0&size=10'
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
- **No inverse collection on `BlogPost`**. The aggregate root carries no `List<Comment>`; comments are fetched through `CommentRepository.findByBlogPost_Id(id, pageable)`. This sidesteps `LazyInitializationException` under `open-in-view: false`, lets the detail endpoint cap the number of comments returned, and keeps writes a single explicit `INSERT` via `commentRepository.save(...)`.
- **Comment count via JPQL subquery** instead of loading every comment — avoids N+1 on the list endpoint.
- **`getReferenceById` when attaching a child**: comment writes set the parent via a Hibernate reference proxy, so there's no extra `SELECT` on the post just to satisfy the FK.
- **`open-in-view: false`** to fail fast on accidental lazy loading outside a transaction.
- **Auditing fields** (`createdAt`, `updatedAt`) populated automatically via `@EnableJpaAuditing`, kept in its own `@Configuration` so `@WebMvcTest` slices don't drag in JPA.
- **Optimistic locking** via `@Version` on `BlogPost`.
- **Global exception handler** returns `ProblemDetail` for not-found, validation, malformed JSON, and type mismatch.

## Next steps if I had more time

- **Persistence**: swap H2 for PostgreSQL via Docker Compose, manage schema with **Flyway**, and use **Testcontainers** for integration tests against a real Postgres.
- **Update / delete endpoints** for posts and comments (`PUT /api/posts/{id}`, `DELETE /api/posts/{id}`, `DELETE /api/posts/{id}/comments/{commentId}`), with proper authorization on who can edit/delete.
- **Authentication** with Spring Security (JWT or session) so comments and posts have an authenticated author instead of a free-form `author` string.
- **Cursor / keyset pagination** on the list endpoints — offset paging gets expensive past a few hundred thousand rows.
- **Caching** of the list endpoint with Spring Cache + Caffeine, invalidated on writes.
- **Observability**: Spring Boot Actuator + Micrometer with Prometheus, structured JSON logs, request tracing via `traceparent`.
- **CI**: GitHub Actions workflow running `./mvnw verify` on every PR, plus JaCoCo for coverage and Spotless/Checkstyle for formatting.
- **Container image**: `Dockerfile` (or `spring-boot:build-image`) so the API can be deployed anywhere a JVM is not pre-installed.
- **Rate limiting** on the public write endpoints (e.g. Bucket4j) to keep abusive clients in check.
