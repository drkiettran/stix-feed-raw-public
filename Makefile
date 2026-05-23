SHELL := /bin/bash

.PHONY: help build test run clean smoke compose-up compose-down

help:
	@echo "Targets:"
	@echo "  build         - mvn clean package"
	@echo "  test          - mvn test"
	@echo "  run           - run the shaded jar locally (needs JWT_SECRET, Kafka)"
	@echo "  compose-up    - docker compose up --build"
	@echo "  compose-down  - docker compose down -v"
	@echo "  smoke         - run scripts/smoke.sh against http://localhost:8080"
	@echo "  clean         - mvn clean"

build:
	mvn -B clean package

test:
	mvn -B test

run:
	@if [ -z "$$JWT_SECRET" ]; then \
	  echo "JWT_SECRET must be set (>=32 bytes)"; exit 1; \
	fi
	java -jar target/stix-feed-raw-1.0.0.jar

compose-up:
	docker compose up --build

compose-down:
	docker compose down -v

smoke:
	./scripts/smoke.sh

clean:
	mvn -B clean
