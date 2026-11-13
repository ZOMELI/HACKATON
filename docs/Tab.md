# Plan SDD del prototipo People & Capacity

Nov 13, 2026 · @Betzy

## Decisión de arquitectura

El prototipo son 6 servicios pequeños para que cinco personas construyan en paralelo, cada una dueña de un servicio y de su spec. Los servicios se integran por contratos REST que se cierran antes de escribir código.

Comparten un solo Postgres con un esquema por servicio. Las migraciones viven en una carpeta aparte, igual que el repositorio de esquemas que ya usan en el banco.

El costo de separar es la integración. Por eso la primera hora del día es para contratos, cada servicio publica un esqueleto con datos fijos en la hora 2, y hay dos puntos de control de integración.

Kafka queda fuera del prototipo: los servicios se llaman por REST. En producción, los avisos de sincronización terminada, caso abierto y caso resuelto pasarían a tópicos.

## Componentes técnicos

Son 6 servicios, una web, una base de datos, una carpeta de fuentes y un modelo de lenguaje. Todo se levanta con un solo `docker-compose`.

&#91;embedded content: servicios y llamadas principales · 6 servicios y la web\]

El dibujo muestra las llamadas principales: la web consulta a cuatro servicios, solo `ingestion-service` toca las fuentes y los demás leen del libro. La tabla de contratos de más abajo tiene las 13 llamadas.

### Servicios

Todos usan Java 21, Spring Boot 3 y arquitectura hexagonal. El nombre va en inglés y la documentación en español.

| # | Servicio | Responsabilidad | Esquema | Puerto | Dueño |
| --- | --- | --- | --- | --- | --- |
| 1 | `mock-sources` | Sirve las tres APIs simuladas bajo `/jira`, `/siga` y `/preper` desde los JSON del paquete de arranque | Ninguno | 8081 | Persona 1 |
| 2 | `ingestion-service` | Un adaptador por fuente: 5 Excel, 3 APIs y los correos. Enlaza a cada persona por matrícula y correo. Envía afirmaciones al libro y necesidades a demanda | `ingestion` | 8082 | Persona 1 |
| 3 | `ledger-service` | Libro de afirmaciones: solo agrega, rechaza la fuente no autorizada para una dimensión y entrega ficha, historial y auditoría | `ledger` | 8083 | Persona 2 |
| 4 | `cases-service` | Reglas de conciliación, casos, alertas de vencimiento configurables y decisión del responsable | `cases` | 8084 | Persona 3 |
| 5 | `demand-service` | Necesidades, estado de Reclutamiento y candidatos por mejor coincidencia de rol | `demand` | 8085 | Persona 4 |
| 6 | `agents-service` | Agente de evidencia y agente de expediente. Es el único que llama al modelo de lenguaje | `agents` | 8086 | Persona 4 |

### Otras piezas

| Pieza | Qué es | Tecnología | Dueño |
| --- | --- | --- | --- |
| `web` | Bandeja de casos, ficha, disponibilidad, demanda y auditoría. Puerto 5173 | React, Vite, TypeScript | Persona 5 |
| `postgres` | Una sola base con un esquema por servicio. Puerto 5432 | PostgreSQL 16, migraciones Flyway en `database/` | Persona 2 |
| Carpeta `fuentes/` | Excel de las PowerApps y correos en texto. En el banco sería SharePoint y un buzón compartido | Volumen montado en `ingestion-service` | Persona 1 |
| Modelo de lenguaje | Único componente externo | El que habilite el banco (por ejemplo Azure OpenAI), vía Spring AI | Persona 4 |

### Contratos entre servicios

Son 13 llamadas. Cada una se define en OpenAPI dentro de `contracts/` antes de implementar.

| Quién llama | A quién | Operación | Para qué |
| --- | --- | --- | --- |
| `web` | `ingestion-service` | `POST /sync` | Botón Sincronizar fuentes |
| `ingestion-service` | `mock-sources` | `GET` de Jira, SIGA y PREPER | Leer las fuentes con API |
| `ingestion-service` | `agents-service` | `POST /evidence-extractions` | Convertir un correo en evidencia estructurada |
| `ingestion-service` | `ledger-service` | `POST /claims/batch` | Registrar lo que dice cada fuente |
| `ingestion-service` | `demand-service` | `PUT /needs` | Entregar necesidades y seguimiento de Reclutamiento |
| `ingestion-service` | `cases-service` | `POST /reconciliations` | Avisar que hay datos nuevos para conciliar |
| `cases-service` | `ledger-service` | `GET /claims` y `POST /claims` | Leer afirmaciones y registrar cada decisión |
| `cases-service` | `agents-service` | `POST /case-summaries` | Redactar el caso recién abierto |
| `demand-service` | `ledger-service` | `GET /claims` | Leer la disponibilidad prevista y confirmada |
| `ledger-service` | `mock-sources` | `GET` del grado en SIGA | Consultar el grado salarial sin guardarlo |
| `web` | `ledger-service` | `GET /people/{id}/profile`, `/history` y `/audit` | Ficha, historial y auditoría |
| `web` | `cases-service` | `GET /cases`, `POST /cases/{id}/decisions`, `GET /availability`, `PUT /alert-settings` | Bandeja, decisión, disponibilidad y días de alerta |
| `web` | `demand-service` | `GET /needs` y `POST /needs` | Tablero de demanda y necesidad informal |

## Los dos agentes de IA

Los dos viven en `agents-service`. Leen y redactan, pero no detectan ni deciden.

|  | Agente de evidencia | Agente de expediente |
| --- | --- | --- |
| Lo llama | `ingestion-service`, una vez por correo | `cases-service`, al abrir un caso |
| Recibe | El texto del correo | El caso, las afirmaciones en conflicto y la evidencia |
| Devuelve | Tipo de correo, matrículas, id de movimiento o acta, fechas, quién aprueba y la cita textual que lo respalda | Resumen del caso en lenguaje claro y el texto de las opciones que las reglas ya definieron |
| Tipos que reconoce | Aprobación de movimiento, conformidad de enroque, renovación de acta, reemplazo en acta, apoyo temporal, necesidad informal, irrelevante | Los ocho tipos de caso |
| No puede | Crear o cambiar afirmaciones de otras dimensiones. Solo adjunta evidencia | Elegir la opción ni cerrar el caso |
| Respaldo sin modelo | Extracciones grabadas en un archivo | Plantilla de texto por tipo de caso |

Tres reglas comunes:

- La salida se valida contra un esquema. Si no cumple, se descarta y se usa el respaldo.
- Todo texto generado se muestra con la marca "redactado por IA".
- Una variable `AGENTS_MODE` cambia entre `live` y `recorded`. El día de la demo, `recorded` es el seguro si falla la red o el modelo.

## La web

Son 5 pantallas y una barra superior. La bandeja es la pantalla de inicio, porque el producto lleva los casos al responsable en lugar de esperar consultas.

| # | Pantalla | Resultado del reto | Qué muestra | Qué permite | Servicio |
| --- | --- | --- | --- | --- | --- |
| 1 | Bandeja de casos | Trazabilidad y disponibilidad | Casos por tipo y fecha límite. El detalle pone lado a lado las afirmaciones en conflicto con su fuente y fecha, la evidencia y el resumen redactado | Decidir con un clic entre las opciones del caso | `cases-service` |
| 2 | Ficha del Team Member | Trazabilidad | Las cuatro dimensiones, cada una con valor, fuente, fecha y estado. Debajo, la línea de tiempo de cambios y los casos abiertos | Buscar por matrícula, nombre o correo | `ledger-service` |
| 3 | Disponibilidad | Disponibilidad | Lista por fecha: persona, desde cuándo, prevista o confirmada y quién debe confirmar | Cambiar los días de anticipación de la alerta | `cases-service` |
| 4 | Demanda | Demanda | Necesidades con tribu o proyecto, perfil, mecanismo y estado de Reclutamiento con su fuente y fecha. Al lado, los candidatos ordenados y la razón de cada uno | Registrar una necesidad informal | `demand-service` |
| 5 | Auditoría | Los tres | Decisiones y movimientos con responsable, fecha y evidencia. Resalta los movimientos aprobados sin evidencia | Filtrar por persona, tipo y fecha | `ledger-service` |

La barra superior tiene dos controles:

- **Sincronizar fuentes.** Dispara la ingesta y muestra qué fuente se leyó y cuándo.
- **Usuario simulado.** Cambia entre un CL, un CAL y el gerente. Sirve para demostrar que cada uno ve solo a su gente y que el grado salarial queda oculto para el CL.

## Plan SDD: organización y constitución

Un monorepo, una constitución compartida y una feature de Spec Kit por servicio. Así cada dueño corre sus fases en su rama sin esperar a los demás.

```text
people-capacity/
  .specify/        constitución y plantillas de Spec Kit
  specs/           una carpeta por feature
  contracts/       OpenAPI de cada servicio: la fuente de verdad de la integración
  database/        migraciones Flyway, una carpeta por esquema
  fuentes/         paquete de arranque: excel, correos y api
  esperado/        casos_esperados.json: el criterio de aceptación
  services/
    mock-sources/  ingestion-service/  ledger-service/
    cases-service/ demand-service/     agents-service/
  web/
  docker-compose.yml
```

| Feature | Quién | Fases |
| --- | --- | --- |
| `001-contracts` | Todo el equipo, en la primera hora | specify y plan. El resultado son los OpenAPI en `contracts/` |
| `002-mock-sources` | Persona 1 | Sin SDD completo: se genera desde `contracts/` y `fuentes/api/` |
| `003-ingestion-service` | Persona 1 | Las seis fases |
| `004-ledger-service` | Persona 2 | Las seis fases |
| `005-cases-service` | Persona 3 | Las seis fases |
| `006-demand-service` | Persona 4 | Las seis fases |
| `007-agents-service` | Persona 4 | Las seis fases |
| `008-web` | Persona 5 | Las seis fases |

### Fase 1: constitución

Se ejecuta una sola vez para todo el repositorio. Contiene principios de ingeniería y ninguna decisión de tecnología.

```text
/speckit-constitution
Crea la constitución del proyecto "CIO People & Capacity Intelligence", un prototipo de un día con datos simulados para gestionar la trazabilidad, la disponibilidad y la demanda de personal de la CIO. Usa estos principios de ingeniería, sin decisiones de tecnología:

1. Procedencia obligatoria. Ningún dato se guarda ni se muestra sin su fuente y su fecha de consulta.
2. Autoridad por dimensión. Pertenencia organizacional, responsabilidad de gestión, origen presupuestal y asignación operativa son dimensiones distintas. Cada una solo acepta datos de su fuente autorizada. Jira nunca determina el presupuesto.
3. Nada se sobrescribe. El historial solo agrega. Una corrección es un registro nuevo que referencia al anterior.
4. Sin suposiciones. Si falta un dato o dos fuentes discrepan, el estado es "pendiente de confirmar" y se muestran ambas versiones. El sistema nunca completa ni elige por su cuenta.
5. La persona decide. Las reglas detectan, la IA lee y redacta, y solo un responsable identificado decide. Ningún presupuesto, pertenencia o disponibilidad cambia de forma automática.
6. Confidencialidad por diseño. Los grados salariales no se copian fuera de su sistema de referencia y solo se consultan según el rol y la jerarquía de quien pregunta.
7. Contrato primero. Ningún servicio se implementa sin su contrato publicado. Un cambio de contrato se acuerda antes de cambiar código.
8. Aceptación por casos. Una regla de negocio está terminada cuando detecta sus casos esperados y no marca los casos negativos del conjunto de aceptación.
9. Degradación segura. Si el modelo de lenguaje o una fuente no responde, el flujo continúa con un respaldo determinista y lo indica.
10. Alcance de un día. Se prefiere un flujo completo de punta a punta sobre funcionalidades parciales. Lo que no aporta a la demostración queda fuera.

Convenciones: documentación y mensajes de commit en español; identificadores de código en inglés.
Gobierno: quien lidera el equipo arbitra los cambios de contrato y de constitución.
```
