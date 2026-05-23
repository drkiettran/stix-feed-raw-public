# Multi-stage build: compile with Maven, run on slim JRE
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src

# Cache dependencies in their own layer
RUN apk add --no-cache maven=~3.9 || apk add --no-cache maven
RUN mvn -B -e -ntp dependency:go-offline
RUN mvn -B -e -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/stix-feed-raw-1.0.0.jar app.jar

EXPOSE 8080
ENV SERVER_PORT=8080 \
    THREAD_POOL_SIZE=50 \
    KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    KAFKA_TOPIC=stix.indicators.v1 \
    JWT_TTL_SECONDS=3600 \
    JWT_ISSUER=stix-feed-raw

# JWT_SECRET must be provided at runtime (no default).
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar"]
