# web — CIO People & Capacity Intelligence

Interfaz del prototipo: trazabilidad, disponibilidad y demanda de personal de la CIO.
React + Vite + TypeScript, sin librería de componentes. Toda la interfaz está en español.

## Cómo correr

```bash
cd web
npm install
npm run dev        # http://localhost:5173
npm run build      # chequeo de tipos (tsc) + build de producción en dist/
```

Para probar sin ningún servicio levantado (todo simulado):

```bash
VITE_MOCK_CASES=true VITE_MOCK_DEMAND=true VITE_MOCK_LEDGER=true VITE_MOCK_INGESTION=true npm run dev
```

## Pantallas

| Ruta | Pantalla | Servicios |
| --- | --- | --- |
| `/`, `/cases/:id` | Bandeja de casos y detalle con decisión | cases |
| `/people` | Ficha del Team Member | ledger, cases |
| `/availability` | Disponibilidad y días de anticipación | cases |
| `/demand` | Demanda, candidatos y necesidad informal | demand |
| `/audit` | Auditoría | ledger |

La barra superior tiene **Sincronizar fuentes** (ingestion) y el **Usuario simulado**. El usuario elegido
se envía en todas las llamadas como `X-User-Id` y `X-User-Role`, se guarda en `localStorage` y al cambiarlo
se recargan los datos. La web no llama a agents-service.

## Variables

Se leen en tiempo de build/arranque de Vite (ver `.env.example`).

| Variable | Por defecto | Uso |
| --- | --- | --- |
| `VITE_CASES_URL` | `http://localhost:8084` | cases-service |
| `VITE_DEMAND_URL` | `http://localhost:8085` | demand-service |
| `VITE_LEDGER_URL` | `http://localhost:8083` | ledger-service |
| `VITE_INGESTION_URL` | `http://localhost:8082` | ingestion-service |
| `VITE_MOCK_CASES`, `VITE_MOCK_DEMAND`, `VITE_MOCK_LEDGER`, `VITE_MOCK_INGESTION` | `false` | `true` reemplaza ese servicio por su mock |

`.env.development` deja `ledger` e `ingestion` simulados y `cases` y `demand` reales. Para cambiarlo sin
tocar ese archivo, crea `.env.local` o pasa las variables en la línea de comandos.

El navegador llama a los servicios directamente, así que cada servicio real debe permitir CORS desde
`http://localhost:5173`, incluidas las cabeceras `X-User-Id`, `X-User-Role` y `Content-Type`.

## Modo mock

- Cada servicio se simula por separado: `src/api/index.ts` elige entre el cliente HTTP (`src/api/*.ts`) y
  el mock (`src/mocks/*.ts`), que implementan la misma interfaz.
- Los datos parten de `src/mocks/data/claims.json` (copia de `contracts/examples/claims.json`) y de los
  8 casos de `esperado/casos_esperados.json`.
- El estado vive en memoria y se pierde al recargar la página: decidir un caso lo resuelve (y, si el
  libro también está simulado, la decisión aparece en Auditoría y en el historial de la persona); crear
  una necesidad la agrega; el perfil oculta el grado salarial al rol CL; el gerente ve todos los casos y
  los demás solo los suyos o los de alguien a su cargo.
- Cuando una pantalla usa algún mock muestra la etiqueta **Datos simulados** junto al título.

## Docker

```bash
docker build -t cio-web ./web \
  --build-arg VITE_CASES_URL=http://localhost:8084 \
  --build-arg VITE_MOCK_LEDGER=true --build-arg VITE_MOCK_INGESTION=true
docker run --rm -p 5173:80 cio-web
```

Build con Node y servido con nginx, con fallback SPA (`nginx.conf`). Las variables `VITE_*` se fijan al
construir la imagen, no al arrancarla; sin `--build-arg`, todos los mocks quedan en `false`.
