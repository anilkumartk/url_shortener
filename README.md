# URL Shortener

A production-style URL shortening service built with Spring Boot, PostgreSQL, and Redis — featuring cache-aside caching, Redis-backed rate limiting, and a fully containerized deployment via Docker Compose.

## Features

- **URL shortening & redirection** — generates unique 7-character short codes and redirects (HTTP 302) to the original URL
- **Redis caching (cache-aside pattern)** — reduces database load by serving frequently-accessed short URLs from Redis, with automatic cache population on miss and 24-hour TTL
- **Rate limiting** — Redis-backed, atomic per-IP request throttling (10 requests/minute) to prevent abuse, using `INCR` + `EXPIRE` for a thread-safe sliding window
- **Click analytics** — tracks and exposes click counts per short URL via a `/stats` endpoint
- **Fully containerized** — one-command startup with Docker Compose (app + PostgreSQL + Redis), using a multi-stage Docker build for a lean production image
- **Environment-based secrets** — database credentials managed via `.env`, never committed to source control

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot, Spring Data JPA, Spring Data Redis |
| Database | PostgreSQL |
| Cache / Rate Limiter | Redis |
| Containerization | Docker, Docker Compose |
| Build Tool | Maven |

## Architecture

```
Client
  │
  ▼
Spring Boot REST API
  │
  ├──▶ Redis (cache-aside: short code → URL, 24h TTL)
  │       │
  │       ▼ (on cache miss)
  ├──▶ PostgreSQL (persistent short code → URL mapping, click counts)
  │
  └──▶ Redis (rate limiter: per-IP request counter, 1-min window)
```

**Why Redis for caching:** URL redirects are a read-heavy, low-latency workload — checking Redis first avoids a database round-trip on every redirect, and the TTL keeps the cache self-cleaning without manual invalidation.

**Why Redis for rate limiting:** Redis's atomic `INCR` command means concurrent requests from the same IP are counted correctly with no race conditions, and `EXPIRE` gives a self-resetting time window with zero cleanup logic needed.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/shorten` | Create a short URL. Body: `{"url": "https://example.com"}` |
| `GET` | `/{shortCode}` | Redirects to the original URL |
| `GET` | `/stats/{shortCode}` | Returns click count, original URL, and creation date |

## Getting Started

### Prerequisites

- Docker and Docker Compose installed

### Run locally

1. Clone the repo:
   ```bash
   git clone https://github.com/<anilkumartk>/url_shortener.git
   cd url_shortener
   ```

2. Create a `.env` file in the project root:
   ```
   POSTGRES_PASSWORD=your_password_here
   ```
   (see `.env.example` for the required format)

3. Start the full stack:
   ```bash
   docker-compose up --build
   ```

4. The app is now running at `http://localhost:8080`

### Example usage

**Shorten a URL:**
```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.google.com"}'
```

**Check stats:**
```bash
curl http://localhost:8080/stats/{shortCode}
```

## What I Learned / Engineering Decisions

- Implemented the **cache-aside pattern** from scratch, including cache population on miss and TTL-based expiry, rather than using a pre-built caching abstraction — to understand the actual mechanics of the pattern.
- Built rate limiting using raw Redis commands (`INCR`, `EXPIRE`) instead of a library, to reason directly about atomicity and race conditions in a concurrent environment.
- Used a **multi-stage Docker build** to separate the JDK-based compilation stage from a lightweight JRE runtime image, reducing final image size.
- Managed secrets via environment variables and `.env` (gitignored), with `.env.example` documenting required configuration for anyone cloning the repo.

## Future Improvements

- Custom short code aliases
- Link expiration dates
- Deployed live instance (Render/Railway)
- Integration tests with Testcontainers

## License

MIT
