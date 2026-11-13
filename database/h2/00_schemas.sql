-- Una sola base de datos compartida, con un esquema por microservicio.
-- Cada servicio se conecta con SCHEMA=<SU_ESQUEMA> en la URL y solo escribe en su esquema.
CREATE SCHEMA IF NOT EXISTS LEDGER;
CREATE SCHEMA IF NOT EXISTS CASES;
CREATE SCHEMA IF NOT EXISTS DEMAND;
CREATE SCHEMA IF NOT EXISTS AGENTS;
CREATE SCHEMA IF NOT EXISTS INGESTION;
