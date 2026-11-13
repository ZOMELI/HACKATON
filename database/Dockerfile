# Base H2 compartida en modo servidor. Todos los microservicios se conectan por TCP al puerto 9092.
FROM eclipse-temurin:21-jre
# Debe coincidir con la versión de H2 que resuelve Quarkus:
#   mvn -f services/ledger-service dependency:tree | grep h2
ARG H2_VERSION=2.3.232
ADD https://repo1.maven.org/maven2/com/h2database/h2/${H2_VERSION}/h2-${H2_VERSION}.jar /opt/h2/h2.jar
COPY h2/ /opt/h2/init/
COPY entrypoint.sh /opt/h2/entrypoint.sh
RUN chmod +x /opt/h2/entrypoint.sh && chmod 644 /opt/h2/h2.jar && mkdir -p /data
EXPOSE 9092 8090
ENTRYPOINT ["/opt/h2/entrypoint.sh"]
