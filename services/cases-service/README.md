# cases-service

Reglas de conciliación, casos, alertas de vencimiento configurables y decisión del responsable. Puerto **8084**.
Contrato: [`contracts/cases.yaml`](../../contracts/cases.yaml).

Las reglas **detectan**, el agente de expediente **redacta** y solo una persona identificada **decide**.
El servicio nunca cierra un caso ni cambia una dimensión por su cuenta.

## Los ocho tipos de caso

| Tipo | Se abre cuando | No se abre si |
| --- | --- | --- |
| `BUDGET_ASSIGNMENT_MISMATCH` | Jira lo ubica en una tribu distinta a la que lo financia en PREPER | Hay un apoyo temporal hacia esa tribu |
| `MOVEMENT_NOT_REFLECTED` | Un movimiento aprobado ya es efectivo y SIGA sigue mostrando la tribu anterior | SIGA ya lo refleja o la fecha es futura |
| `MANAGER_MISMATCH` | SIGA y el Excel de chapters nombran responsables distintos | Coinciden |
| `MOVEMENT_WITHOUT_EVIDENCE` | Un movimiento figura aprobado sin correo de aprobación | Trae evidencia `MOVEMENT_APPROVAL` |
| `SWAP_INCOMPLETE` | En un enroque falta la conformidad del responsable de la persona | Su responsable ya dio conformidad |
| `CONTRACT_EXPIRING` | El acta venció o vence dentro de `contractExpiryDays` | Hay correo de renovación o reemplazo |
| `AVAILABILITY_UNCONFIRMED` | Hay disponibilidad prevista dentro de `availabilityDays` | Ya está confirmada |
| `TEMP_SUPPORT_EXPIRED` | El apoyo temporal venció y sigue asignado a la tribu anfitriona | El apoyo sigue vigente |

El criterio de aceptación es [`esperado/casos_esperados.json`](../../esperado/casos_esperados.json): `ReconciliationRulesTest`
exige los 8 casos esperados y ningún negativo.

## Cómo correr

```bash
# Sin Docker: H2 en memoria, libro simulado y conciliación al arrancar
mvn spring-boot:run -Dspring-boot.run.profiles=local

curl localhost:8084/cases -H 'X-User-Id: L20001' -H 'X-User-Role: CL'
curl -X POST localhost:8084/reconciliations
mvn verify
```

## Variables

| Variable | Por defecto | Para qué |
| --- | --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | `localhost:5433/hackaton`, `assistant` | Esquema `cases` (migración en `database/cases/`) |
| `LEDGER_MODE` | `stub` | `stub` = afirmaciones de ejemplo en memoria; `http` = ledger-service real |
| `LEDGER_URL` | `http://localhost:8083` | Solo con `LEDGER_MODE=http` |
| `AGENTS_URL` | `http://localhost:8086` | agents-service |
| `AGENTS_ENABLED` | `true` | `false` = siempre la plantilla local |
| `CASES_REFERENCE_DATE` | vacío | Fija "hoy" (`yyyy-MM-dd`) para que la demo no dependa del día |
| `CASES_RECONCILE_ON_STARTUP` | `false` (`true` en `local`) | Concilia al arrancar |
| `CASES_DEFAULT_OWNER_ID` | `G40001` | Quién decide si la persona no tiene responsable en el libro |

## Comportamiento que conviene conocer

- **Un caso no se reabre.** Cada caso tiene una clave (tipo, persona y el dato en conflicto). Si ya existe, abierto o resuelto, la conciliación lo cuenta como conocido.
- **Decidir registra en el libro primero.** Se publica una afirmación `DECISION` y, si la opción tiene un efecto inequívoco (confirmar disponibilidad, elegir responsable), una segunda afirmación con `supersedes`. Si el libro falla responde 502 y el caso sigue abierto.
- **Visibilidad.** Cada usuario ve los casos que le toca decidir y los de quienes tiene a cargo; `MANAGER` (o sin cabecera) ve todo. La jerarquía se deduce de las afirmaciones `MANAGEMENT_RESPONSIBILITY`.
- **Degradación segura.** Si agents-service no responde o devuelve opciones distintas a las de la regla, el resumen sale de una plantilla local con `mode=FALLBACK` y `aiGenerated=false`.

## Estructura

```
domain/         modelo y ReconciliationRules (sin Spring)
application/    casos de uso (port.in), puertos de salida (port.out) y servicios
infrastructure/ web, persistence, ledger (http | stub), agents, config
```
