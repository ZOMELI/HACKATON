-- Esquema DEMAND (demand-service): necesidades de personal. Los candidatos se calculan al consultar.
SET SCHEMA DEMAND;

-- MECHANISM: PREPER_BOX, PROVIDER_ACT, UNDEFINED.
-- ORIGIN:    STAFFING_PORTAL, PROVIDER_ACT, INFORMAL.
-- RECRUITMENT_STATUS: el que informa Reclutamiento, o PENDING_CONFIRMATION si no lo ha informado.
-- MECHANISM_VALID: FALSE cuando la caja o el acta de respaldo no existe en su fuente.
CREATE TABLE IF NOT EXISTS NEED (
    NEED_ID                 VARCHAR(60)  NOT NULL PRIMARY KEY,
    TRIBE                   VARCHAR(100),
    SQUAD                   VARCHAR(100),
    PROJECT                 VARCHAR(200),
    PROFILE                 VARCHAR(100) NOT NULL,
    QUANTITY                INT,
    MECHANISM               VARCHAR(30),
    MECHANISM_REF           VARCHAR(60),
    MECHANISM_VALID         BOOLEAN,
    REQUIRED_DATE           DATE,
    REQUESTER               VARCHAR(200),
    ORIGIN                  VARCHAR(30)  NOT NULL,
    RECRUITMENT_STATUS      VARCHAR(40)  NOT NULL,
    RECRUITMENT_UPDATED_AT  DATE
);

CREATE INDEX IF NOT EXISTS IDX_NEED_ORIGIN ON NEED (ORIGIN);
