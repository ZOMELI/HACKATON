#!/bin/sh
# Aplica los scripts (son idempotentes) y luego arranca H2 en modo servidor.
set -e
PASSWORD="${H2_PASSWORD:-peoplecapacity}"
for script in /opt/h2/init/*.sql; do
  echo "Aplicando $script"
  java -cp /opt/h2/h2.jar org.h2.tools.RunScript \
    -url "jdbc:h2:file:/data/peoplecapacity" -user sa -password "$PASSWORD" -script "$script"
done
# TCP 9092 para los servicios; consola web en 8090 (URL JDBC: jdbc:h2:tcp://localhost:9092/peoplecapacity).
exec java -cp /opt/h2/h2.jar org.h2.tools.Server \
  -tcp -tcpAllowOthers -tcpPort 9092 \
  -web -webAllowOthers -webPort 8090 \
  -baseDir /data -ifNotExists
