#!/bin/sh
# Compila los 6 servicios y levanta todo con docker compose.
set -e
cd "$(dirname "$0")/.."
mvn -q package
docker compose up --build
