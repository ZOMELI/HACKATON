# Contexto de build: web/   ->   docker build -t cio-web ./web
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

# Las variables VITE_* se resuelven en tiempo de build.
ARG VITE_CASES_URL=http://localhost:8084
ARG VITE_DEMAND_URL=http://localhost:8085
ARG VITE_LEDGER_URL=http://localhost:8083
ARG VITE_INGESTION_URL=http://localhost:8082
ENV VITE_CASES_URL=$VITE_CASES_URL \
    VITE_DEMAND_URL=$VITE_DEMAND_URL \
    VITE_LEDGER_URL=$VITE_LEDGER_URL \
    VITE_INGESTION_URL=$VITE_INGESTION_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
