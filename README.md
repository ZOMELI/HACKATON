# People & Capacity: código base de los 6 microservicios

Código base en Quarkus 3.33.4.2 (Java 21) con arquitectura hexagonal, contrato OpenAPI primero,
una base H2 compartida y docker-compose. Es un punto de partida: cada servicio arranca con un flujo
mínimo que funciona de punta a punta y deja marcadas las reglas que faltan construir con SDD.

## Estado de verificación

Este código **no se compiló con Maven ni se ejecutó en Quarkus**: el entorno donde se generó no tiene
acceso a Maven Central. Lo que sí se comprobó:

- Los 6 `openapi.yml` son OpenAPI 3.0 válidos.
- Las interfaces JAX-RS se generaron con openapi-generator 7.27.0 (la misma versión del `pom.xml`)
  y las 85 clases Java compilan con `javac` contra esas interfaces y contra plantillas de las APIs
  de Jakarta, Panache y MicroProfile.
- Cada columna mapeada en las entidades existe en los scripts SQL, y viceversa.
- El extractor de evidencia clasifica bien los 8 correos del paquete simulado.

Lo que falta comprobar en la primera compilación real:

- Que la plataforma Quarkus `3.33.4.2` resuelva en el repositorio Maven del banco.
- Que los scripts SQL corran en H2 (están escritos en SQL estándar, sin probar contra H2).
- Que la versión del servidor H2 (`database/Dockerfile`, `H2_VERSION`) coincida con la que trae Quarkus.
- El arranque de Hibernate, los clientes REST y docker-compose.

## Cómo levantarlo

```bash
mvn package            # compila los 6 servicios (pom agregador en la raíz)
docker compose up --build
```

O en un paso: `scripts/build-all.sh`. Luego:

```bash
curl -X POST http://localhost:8082/sync        # lee PREPER y SIGA, registra afirmaciones y concilia
curl http://localhost:8084/cases               # casos abiertos
curl http://localhost:8084/availability        # disponibilidad prevista
curl http://localhost:8083/people/B10576/profile
```

Cada servicio publica su contrato en `/q/openapi` y Swagger UI en `/q/swagger-ui`.
La consola de H2 queda en http://localhost:8090 (URL `jdbc:h2:tcp://localhost:9092/peoplecapacity`, usuario `sa`).

## Servicios

| Servicio | Puerto | Esquema H2 | Qué hace hoy | Pendiente con SDD |
|---|---|---|---|---|
| `mock-sources` | 8081 | ninguno | Sirve Jira, SIGA y PREPER desde los JSON simulados; controla el permiso del grado salarial | Nada |
| `ingestion-service` | 8082 | `INGESTION` | Lee PREPER y SIGA, envía afirmaciones al libro, dispara la conciliación y guarda el resultado por fuente | Lectores de Jira, los 5 Excel y los correos; enlace de identidad por correo |
| `ledger-service` | 8083 | `LEDGER` | Registra afirmaciones (solo agrega), rechaza fuentes no autorizadas por dimensión, arma la ficha y consulta el grado sin guardarlo | Control de acceso por jerarquía |
| `cases-service` | 8084 | `CASES` | Regla de vencimientos, bandeja, decisión que vuelve al libro, disponibilidad y días de alerta configurables | Las otras 7 reglas de conciliación |
| `demand-service` | 8085 | `DEMAND` | Guarda necesidades y propone candidatos por coincidencia de rol | Señalar cajas inexistentes; experiencia en Jira como criterio |
| `agents-service` | 8086 | `AGENTS` | Extrae evidencia de correos y redacta resúmenes con un respaldo determinista (`AGENTS_MODE=recorded`) | Conectar el modelo de lenguaje en `NoModelAdapter` |

## Contract-first

El contrato de cada servicio es `src/main/resources/META-INF/openapi.yml`. En cada compilación,
`openapi-generator-maven-plugin` genera desde ese archivo la interfaz JAX-RS y los DTO en
`target/generated-sources/openapi`. El adaptador REST (`*Resource`) implementa la interfaz generada,
así que un cambio de contrato que el código no cumple rompe la compilación.

La carpeta `contracts/` tiene una copia de los 6 contratos para consulta. La fuente de verdad es el
archivo dentro de cada servicio; si cambias uno, vuelve a copiarlo.

## Arquitectura hexagonal

Todos los servicios usan el mismo esquema de paquetes bajo `com.peoplecapacity.<servicio>`:

```
domain/model            registros de dominio, sin dependencias de framework
domain/service          reglas de negocio puras (política de fuentes, reglas de conciliación, ranking)
application/port/in     casos de uso que ofrece el servicio
application/port/out    lo que el servicio necesita de afuera (repositorios, otros servicios, modelo)
application/usecase     implementación de los casos de uso
infrastructure/adapter/in/rest           recurso REST que implementa la interfaz generada
infrastructure/adapter/out/persistence   entidades JPA y repositorios Panache
infrastructure/adapter/out/client        clientes REST hacia otros servicios
```

## Base de datos

Una sola base H2 en modo servidor (contenedor `h2`), con un esquema por servicio. Los scripts están
en `database/h2/` y se aplican en orden cada vez que arranca el contenedor; son idempotentes.

| Script | Esquema | Tablas |
|---|---|---|
| `00_schemas.sql` | todos | crea los 5 esquemas |
| `10_ledger.sql` | `LEDGER` | `PERSON`, `CLAIM` |
| `20_cases.sql` | `CASES` | `CASE_FILE`, `CASE_PERSON`, `CASE_OPTION`, `DECISION`, `ALERT_SETTING` |
| `30_demand.sql` | `DEMAND` | `NEED` |
| `40_agents.sql` | `AGENTS` | `EVIDENCE_EXTRACTION`, `CASE_SUMMARY` |
| `50_ingestion.sql` | `INGESTION` | `SYNC_RUN`, `SYNC_SOURCE_RESULT` |

Hibernate no crea ni modifica tablas: el esquema lo definen los scripts. Cada servicio se conecta con
`SCHEMA=<SU_ESQUEMA>` en la URL JDBC y no lee tablas de otro servicio.

## Decisiones de diseño que conviene revisar

- **Valor de una afirmación.** Es un texto. Para financiamiento lleva varios atributos en la forma
  `ref=PRE-4114;category=Running;tribe=Pagos;type=Permanente`, para que las reglas comparen categorías.
- **Sin llaves foráneas entre `CLAIM` y `PERSON`.** Un usuario de Jira sin enlazar se registra con su correo.
- **Candidatos calculados al consultar.** No hay tabla de candidatos; se derivan del libro en cada consulta.
- **Estado de Reclutamiento dentro de `NEED`.** Va como columnas y no como tabla aparte.
- **Sin Kafka.** Los servicios se llaman por REST; los avisos (sincronización terminada, caso abierto,
  caso resuelto) son los candidatos a tópicos en producción.
- **Sin pruebas automáticas.** El `pom.xml` tiene `skipTests` y no incluye dependencias de prueba.
- **Usuario simulado.** Se recibe en el encabezado `X-User-Id`; no hay autenticación real.

## Otros contenidos

- `fuentes/`: Excel y correos simulados (los JSON de las APIs están dentro de `mock-sources`).
- `esperado/casos_esperados.json`: lo que el prototipo completo debe detectar; sirve como criterio de aceptación.
