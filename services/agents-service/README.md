# agents-service

Agente de evidencia y agente de expediente de "CIO People & Capacity". Es el único servicio que llama al
modelo de lenguaje. Los agentes **leen y redactan**: no detectan casos ni deciden. Contrato: `contracts/agents.yaml`.

| Endpoint | Qué hace |
|---|---|
| `POST /evidence-extractions` | Recibe un correo y devuelve tipo, matrículas, id de movimiento o acta, fechas, quién aprueba y la cita textual que lo respalda. |
| `POST /case-summaries` | Recibe un caso (afirmaciones, evidencia y opciones ya definidas por las reglas) y devuelve un resumen en español y un texto por opción. No elige, agrega ni quita opciones. |
| `GET /actuator/health` | Estado del servicio. |

Puerto `8086`. Sin base de datos. CORS abierto para `http://localhost:5173`. Errores: `{ "message": "..." }`.

## Cómo correr

Requiere Java 21 y Maven 3.9.

```bash
cd services/agents-service
mvn -q verify                                   # compila y ejecuta las pruebas
mvn spring-boot:run                             # arranca en modo recorded, sin clave
# o
java -jar target/agents-service-0.1.0.jar
```

Con Docker (contexto de build: `services/agents-service`):

```bash
docker build -t agents-service services/agents-service
docker run -p 8086:8086 -e AGENTS_MODE=recorded agents-service
```

Prueba rápida:

```bash
curl -s localhost:8086/evidence-extractions -H 'Content-Type: application/json' \
  -d '{"emailId":"MAIL-002","body":"Apruebo el movimiento MOV-102 de Luis Ramos (U10002) a la tribu Riesgos desde el 15 de octubre."}'
```

## Variables

| Variable | Por defecto | Uso |
|---|---|---|
| `AGENTS_MODE` | `recorded` | `live` llama al modelo; `recorded` nunca lo llama. Cualquier otro valor se trata como `recorded`. |
| `OPENAI_API_KEY` | (vacía) | Clave de OpenAI. Solo hace falta en `live`. Sin clave el servicio arranca igual. |
| `OPENAI_MODEL` | `gpt-4o-mini` | Modelo de chat. |
| `OPENAI_BASE_URL` | `https://api.openai.com` | Para apuntar a un proxy o a un servidor de prueba. |
| `AGENTS_LLM_TIMEOUT_SECONDS` | `20` | Tiempo máximo de espera por llamada al modelo. |
| `PORT` | `8086` | Puerto HTTP. |

## Cómo cambiar de modo

```bash
AGENTS_MODE=live OPENAI_API_KEY=sk-... java -jar target/agents-service-0.1.0.jar
```

El modo se lee al arrancar; para cambiarlo hay que reiniciar. Cada respuesta dice cómo se produjo:

- `mode=LIVE`, `aiGenerated=true`: lo redactó el modelo y pasó la validación.
- `mode=RECORDED`, `aiGenerated=false`: respaldo, ya sea por configuración o porque el modelo falló.

## Degradación segura (modo live)

La salida del modelo se valida antes de devolverla. Se descarta y se usa el respaldo si:

- el tipo no está en el catálogo, una matrícula no tiene el formato letra + 5 dígitos, un id no es
  `MOV-nnn` / `ENR-nnn` / `ACT-nnn`, o una fecha no es ISO válida;
- una matrícula o un id no aparecen en el correo, o la cita no aparece literalmente en el cuerpo
  (solo se toleran diferencias de espacios y saltos de línea);
- en un resumen, los ids de opción no son exactamente los recibidos, o algún texto viene vacío;
- el modelo da error, responde algo ilegible o tarda más del tiempo máximo.

En todos esos casos se responde 200 con el respaldo y queda un log `WARN` ("se usó el respaldo…").
Un fallo del modelo nunca produce un 500.

Respaldos:

- **Evidencia**: extracción grabada por `emailId` en `src/main/resources/recorded/evidence-extractions.json`.
  Si el correo no está grabado, una heurística determinista (expresiones regulares y palabras clave);
  sin señal clara devuelve `IRRELEVANT`.
- **Expediente**: una plantilla por cada uno de los ocho tipos de caso, que interpola persona, valores,
  fuentes y fechas de las afirmaciones recibidas. El texto de cada opción es su `label`, más una frase de
  consecuencia cuando la plantilla reconoce la opción. Tipo desconocido: plantilla genérica.

Los prompts tratan el contenido del correo y del caso como datos, no como instrucciones, y prohíben
inventar datos (`infrastructure/llm/Prompts.java`).

## Estructura (hexagonal)

- `domain`: modelos, validadores, heurística y plantillas. Sin Spring.
- `application`: casos de uso y puertos (`port.in`, `port.out`: `LanguageModelPort`, `RecordedExtractionsPort`).
- `infrastructure`: adaptadores `web`, `llm` (OpenAI vía Spring AI `ChatClient`), `recorded` y configuración.

## Correos de ejemplo

`fuentes/correos/MAIL-*.txt` (cabeceras `De`, `Para`, `Asunto`, `Fecha` en ISO 8601, línea en blanco y cuerpo).
Cubren los siete tipos de evidencia y todos tienen su extracción grabada. Una prueba comprueba que cada
cita grabada aparece literalmente en su correo.

## Supuestos

- Las matrículas de `personIds` son las personas de las que trata el correo; la de quien aprueba va solo en `approver`.
- Toda extracción que no sea `IRRELEVANT` debe traer cita; si el modelo no la da, se usa el respaldo.
- Si el correo da día y mes sin año, se toma el año de `receivedAt`; sin `receivedAt` la heurística no pone fecha.
- En modo recorded, un `emailId` grabado devuelve su extracción grabada aunque el `body` enviado sea distinto.
- `options` es obligatorio pero puede ser una lista vacía; cada opción necesita `id` y `label`.
- Los campos que el contrato no define (por ejemplo `claimId` o `sourceRef`) se ignoran sin error.
- Las frases de consecuencia de las opciones se eligen por palabras de su `id` y `label`, porque este
  servicio no conoce el catálogo de opciones de `cases-service`. Si no reconoce la opción, devuelve solo el `label`.
