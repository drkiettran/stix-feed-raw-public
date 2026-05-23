# stix-feed-raw

A reference implementation of a STIX 2.1 Indicator Feed Processor written in Java 21
**without an application framework**. Built with the JDK's built-in HTTP server
(`com.sun.net.httpserver.HttpServer`), the plain `kafka-clients` library, hand-rolled
JWT verification, and explicit dependency wiring in `Main`.

This is the framework-free counterpart to [`stix-feed-boot-public`](https://github.com/drkiettran/stix-feed-boot-public).
Both expose the same HTTP API for direct side-by-side comparison.

---

## What's in here

| Layer        | Files                                           | Purpose                                  |
| ------------ | ----------------------------------------------- | ---------------------------------------- |
| `domain/`    | `Indicator`, `PatternType`, `IndicatorTypeOV`   | STIX 2.1 record + enums                  |
| `validation/`| `IndicatorValidator`, `ValidationResult/Error`  | Hand-rolled validation rules             |
| `store/`     | `InMemoryIndicatorStore`                        | `ConcurrentHashMap` storage + querying   |
| `kafka/`     | `IndicatorPublisher`                            | `kafka-clients` producer (no Spring)     |
| `security/`  | `JwtTokenIssuer/Verifier`, `UserAuthenticator`  | HS256 JWT with `jjwt`                    |
| `json/`      | `JsonMapper`                                    | Jackson facade                           |
| `http/`      | `Router`, `Route`, `RequestContext`, etc.       | Manual routing layer                     |
| `http/filters/` | `Tracing`, `Error`, `Jwt`                    | Filter chain                             |
| `http/handlers/` | `Auth`, `Health`, `Ready`, `IndicatorXxx`   | Endpoint handlers                        |
| `error/`     | `ErrorResponse`, `ErrorDetail`, `ErrorType`     | Standardized error model                 |
| `config/`    | `AppConfig` + records                           | Env-var configuration loader             |
| `Main`       | -                                               | Explicit dependency wiring               |

---

## Build and run on Ubuntu 24

### Prerequisites

```bash
java -version       # need 21+
mvn -version        # need 3.9+ (3.8 also works)
docker --version
docker compose version
```

### Run with Docker Compose (recommended)

Brings up Zookeeper + Kafka + the service in one command.

```bash
docker compose up --build
```

The service listens on `http://localhost:8080`. Kafka is reachable on
`localhost:9094` from the host (and `kafka:9092` from inside the compose network).

### Build and run locally (without Docker)

You'll need a Kafka broker accessible at `KAFKA_BOOTSTRAP_SERVERS`.

```bash
export JWT_SECRET="this-is-a-32-byte-secret-for-dev!"
export KAFKA_BOOTSTRAP_SERVERS="localhost:9094"   # if Kafka via compose

mvn -B clean package
java -jar target/stix-feed-raw-1.0.0.jar
```

### Run the smoke test

In another terminal:

```bash
./scripts/smoke.sh
```

You should see `health`, `ready`, token issuance, POST, GET, and query all succeed.

---

## Run the unit tests

```bash
mvn -B test
```

Three test classes ship out of the box:
- `IndicatorValidatorTest` — every validation rule
- `JwtRoundTripTest` — issuer + verifier with right and wrong secrets
- `RouteTest` — path templates and parameter capture

---

## Environment variables

| Variable                     | Required | Default                | Notes                                    |
| ---------------------------- | -------- | ---------------------- | ---------------------------------------- |
| `JWT_SECRET`                 | yes      | —                      | Must be ≥ 32 bytes (HS256)               |
| `JWT_TTL_SECONDS`            | no       | `3600`                 |                                          |
| `JWT_ISSUER`                 | no       | `stix-feed-raw`        |                                          |
| `SERVER_PORT`                | no       | `8080`                 |                                          |
| `THREAD_POOL_SIZE`           | no       | `50`                   | HttpServer executor                      |
| `KAFKA_BOOTSTRAP_SERVERS`    | no       | `localhost:9092`       |                                          |
| `KAFKA_TOPIC`                | no       | `stix.indicators.v1`   |                                          |
| `SHUTDOWN_GRACE_SECONDS`     | no       | `5`                    | HttpServer.stop drain                    |

---

## Test users (for development)

| Username  | Password         | Roles               |
| --------- | ---------------- | ------------------- |
| `analyst` | `analyst-pass`   | `analyst`, `reader` |
| `reader`  | `reader-pass`    | `reader`            |
| `admin`   | `admin-pass`     | `analyst`, `reader` |

---

## Importing into Eclipse

1. **File → Import → Maven → Existing Maven Projects**
2. Browse to the project root (where `pom.xml` lives)
3. Select the project; Eclipse will resolve dependencies via the m2e plugin
4. **Project Properties → Java Compiler** — confirm the level is **21**
5. **Run As → Maven build...** with goal `clean package` to verify

If Eclipse complains about `--release 21`, install or update **m2e** and the
**JDT compiler** to the latest version. Java 21 support requires Eclipse 2023-06 or later.

To run `Main` from inside Eclipse: right-click `Main.java` → **Run As → Java Application**.
Set environment variables under **Run Configurations → Environment** (notably `JWT_SECRET`).

---

## Known scope limitations (deliberate)

The following are intentionally out of scope:

- Persistent storage (in-memory only)
- TLS termination (assume reverse proxy)
- Token refresh / revocation
- STIX bundles or other SDOs
- Pattern syntax validation
- Rate limiting
- Metrics endpoints

Each is a documented "production consideration," not a bug.

---

## License

MIT License — see the `LICENSE` file at the project root for the full text.
