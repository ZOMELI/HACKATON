# CIO People & Capacity Intelligence

Prototipo de un día con datos simulados para la trazabilidad, la disponibilidad y la demanda de personal de la CIO.
Contiene los seis servicios (mock-sources, ingestion, ledger, cases, demand y agents) y la web. Diseño en `docs/Tab.md`.

## Levantar todo

```bash
docker compose up -d --build
# La base es un Postgres externo (localhost:5433, base `hackaton`). Para usar otra:
DB_URL=jdbc:postgresql://host.docker.internal:5432/otra DB_USER=... DB_PASSWORD=... docker compose up -d --build
```

| Pieza | URL | Notas |
| --- | --- | --- |
| web | http://localhost:5173 | Bandeja de casos, ficha, disponibilidad, demanda, auditoría |
| cases-service | http://localhost:8084 | Concilia al arrancar |
| demand-service | http://localhost:8085 | Siembra 5 necesidades de ejemplo |
| agents-service | http://localhost:8086 | `AGENTS_MODE=recorded` por defecto |
| postgres (externo) | localhost:5433 | Base `hackaton`, usuario `assistant`, esquemas `cases` y `demand` |

Sin Docker, cada servicio corre solo con `mvn spring-boot:run -Dspring-boot.run.profiles=local` (H2 en memoria)
y la web con `npm install && npm run dev` dentro de `web/`.

## Cómo fluye la información

1. **Fuentes**: SIGA, PREPER y Jira (APIs simuladas por `mock-sources`) más los Excel y correos de `fuentes/`.
2. **ingestion-service** (`POST /sync`): un lector por fuente (9 en total). Cada dato se convierte en una afirmación
   para el libro; los correos pasan antes por el agente de evidencia; las necesidades van a demand (`PUT /needs`).
3. **ledger-service**: guarda las afirmaciones sin modificarlas y rechaza la fuente no autorizada para una dimensión.
4. **cases-service**: al terminar la ingesta concilia, abre casos para el responsable y registra cada decisión en el libro.
5. **demand-service**: cruza necesidades con la disponibilidad prevista o confirmada del libro.
6. **web**: bandeja, ficha, disponibilidad, demanda y auditoría sobre los servicios.

En ejecución nada es simulado salvo `mock-sources` (hace de SIGA, PREPER y Jira) y el modo `recorded` de agents.
Los datos de ejemplo que quedan viven solo en `src/test` y los usan las pruebas.

Con las fuentes del paquete se detectan 8 casos (ver `esperado/casos_esperados.json`).

## Variables útiles

| Variable | Por defecto | Uso |
| --- | --- | --- |
| `AGENTS_MODE` | `recorded` | `live` llama al modelo; necesita `OPENAI_API_KEY` en `.env` (no se versiona) |

## Pendientes

- **Spec Kit**: crear la constitución (principios de `docs/Tab.md`) y una especificación por servicio en `specs/`.
- **Web**: "Aceptar hasta" y "Proponer al CL".
- **Modo `recorded`**: grabar las extracciones de los 8 correos nuevos en agents-service.
- **Contrato del libro**: cases registra decisiones por `POST /claims/batch` porque el ledger no expone `POST /claims`.
- **docker-compose**: construir las imágenes de los nueve contenedores y probar el arranque completo.

## Contratos

`contracts/` es la fuente de verdad de la integración. `cases.yaml`, `demand.yaml` y `agents.yaml` son de este equipo.
`ledger.yaml` e `ingestion.yaml` son copia del OpenAPI que publican esos servicios; cases, demand y la web
traducen ese modelo en sus adaptadores (`infrastructure/ledger` y `web/src/api`).

## Fuentes y aceptación

- `fuentes/excel/` y `fuentes/correos/`: los 5 Excel y los 8 correos que lee ingestion-service.
- `esperado/casos_esperados.json`: los 8 casos que deben detectarse con esas fuentes y los 4 que no.
  `cases-service` lo verifica en `RealSourcesAcceptanceTest`.

Usuarios para el selector de la web (existen en SIGA): Tello Espinoza, Carlos `B10132`, Chávez Arce, María `B10169` y Mendoza Núñez, Daniela `B10206` (CL); Mendoza Campos, Carlos `B10058` (CAL); Castillo Pacheco, Milagros `B10021` (gerente).

## Estructura

```
contracts/   OpenAPI de cada servicio
database/    migraciones Flyway, una carpeta por esquema
esperado/    criterio de aceptación
fuentes/     correos de ejemplo
services/    cases-service, demand-service, agents-service
web/         React + Vite + TypeScript
```
