# demand-service

Necesidades de personal, estado de Reclutamiento (siempre con fuente y fecha de consulta) y candidatos
por mejor coincidencia de rol. Puerto **8085**. Contrato: `contracts/demand.yaml`.

| Operación | Qué hace |
|---|---|
| `PUT /needs` | Upsert por `externalId` (lo llama ingestion). Todo o nada: 400 si alguna necesidad no trae `source` u `observedAt`. |
| `POST /needs` | Necesidad informal desde la web: `informal=true`, `source=WEB`, `observedAt`=ahora, `createdBy`=cabecera `X-User-Id`. |
| `GET /needs?status=` | Tablero: cada necesidad con `candidates` ordenados por `score` descendente. |
| `GET /actuator/health` | Salud. |

Errores: JSON `{ "message": "..." }`.

## Candidatos y puntaje

Los candidatos son las personas con una afirmación `AVAILABILITY` vigente en el libro (`GET /claims` de ledger-service).
El puntaje es determinista, de 0 a 100, y `reason` explica cada sumando:

| Criterio | Puntos |
|---|---|
| Rol idéntico (sin distinguir mayúsculas, tildes ni separadores) | 60 |
| Rol de la misma familia (comparte una palabra clave: Developer, Engineer, QA...) | 30 |
| Disponibilidad `CONFIRMED` | 25 |
| Disponibilidad `PLANNED` | 10 |
| Disponible en o antes de `neededFrom` | 15 |

Sin ninguna coincidencia de rol, la persona no aparece. Si el libro no responde, la necesidad sale con
`candidates=[]` y `candidatesAvailable=false` (nunca 500).

## Cómo correr

Sin Docker (H2 en memoria, libro en modo stub, 5 necesidades de ejemplo sembradas):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
curl localhost:8085/needs
curl -X POST localhost:8085/needs -H 'Content-Type: application/json' -H 'X-User-Id: U20001' \
  -d '{"tribe":"Data","role":"Data Engineer","neededFrom":"2026-12-10"}'
```

Con Postgres (perfil por defecto): requiere el esquema `demand` y la migración
`database/demand/V1__demand_init.sql` ya aplicada (lo hace el contenedor Flyway del docker-compose).
El servicio solo valida el esquema (`ddl-auto=validate`).

```bash
mvn -q verify                                   # pruebas unitarias e integración
docker build -t demand-service .                # contexto: services/demand-service
```

## Variables de entorno

| Variable | Por defecto | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/hackaton` | Base de datos (esquema `demand`) |
| `DB_USER` / `DB_PASSWORD` | `assistant` / `assistant` | Credenciales |
| `LEDGER_MODE` | `stub` | `http` = ledger-service real; `stub` = copia en memoria de `contracts/examples/claims.json` |
| `LEDGER_URL` | `http://localhost:8083` | Base del ledger-service (solo modo `http`; timeouts 1 s conexión / 2 s lectura) |

## Arquitectura (hexagonal)

- `domain`: modelo (`Need`, `Candidate`, `AvailabilityClaim`) y `CandidateScorer`. Sin Spring.
- `application`: casos de uso (`port.in`), puertos de salida (`port.out`: `NeedRepository`, `LedgerPort`) y `NeedService`.
- `infrastructure`: `web` (REST), `persistence` (JPA), `ledger` (adaptadores `http` y `stub`), `config` (CORS, semilla local).

## Supuestos

- `POST /needs` exige `role` y además `tribe` o `project` (el contrato marca solo `role` como requerido, pero describe el 400 como "Falta tribu/proyecto o perfil").
- Si `recruitment` llega sin `source` u `observedAt`, hereda los de la necesidad; sin `status` se rechaza con 400.
- `status` por defecto `OPEN` y `mechanism` por defecto `UNDEFINED`. El upsert reemplaza todos los campos de la necesidad.
- `evidenceQuote` se guarda pero no se devuelve, porque el esquema `Need` del contrato no lo incluye.
- Se calculan candidatos para todas las necesidades, también las `COVERED` y `CANCELLED`.
- "Vigente" = afirmación no reemplazada (`current=true`) y con `validUntil` vacío o no vencido. Si hay varias por persona se usa la de `observedAt` más reciente.
- El stub es una copia de `contracts/examples/claims.json`; si el ejemplo cambia hay que volver a copiarlo a `src/main/resources/stub/claims.json`.
- Sin autenticación: `X-User-Id` se toma tal cual (usuario simulado); si falta, `createdBy` queda en `null`.


## Necesidades de ejemplo

`DEMAND_SEED_EXAMPLES=true` siembra cinco necesidades de ejemplo al arrancar (upsert por `externalId`, idempotente). Está activo en el perfil `local` y en `docker-compose.yml`; poner `false` cuando ingestion-service entregue las necesidades reales.
