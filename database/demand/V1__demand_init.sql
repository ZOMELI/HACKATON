-- demand-service: necesidades de personal y su seguimiento en Reclutamiento.
-- El esquema `demand` lo crea el contenedor Flyway (no se crea aquí).

CREATE TABLE demand.needs (
    id                       VARCHAR(36)  PRIMARY KEY,
    external_id              VARCHAR(100),
    informal                 BOOLEAN      NOT NULL DEFAULT FALSE,
    tribe                    VARCHAR(150),
    project                  VARCHAR(200),
    role_name                VARCHAR(150) NOT NULL,
    seniority                VARCHAR(60),
    mechanism                VARCHAR(30)  NOT NULL,
    needed_from              DATE,
    status                   VARCHAR(20)  NOT NULL,
    -- Ningún dato sin fuente y fecha de consulta
    source                   VARCHAR(60)  NOT NULL,
    observed_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Seguimiento de Reclutamiento (las tres columnas en NULL = sin seguimiento)
    recruitment_status       VARCHAR(20),
    recruitment_source       VARCHAR(60),
    recruitment_observed_at  TIMESTAMP WITH TIME ZONE,
    note                     VARCHAR(2000),
    evidence_quote           VARCHAR(2000),
    created_by               VARCHAR(60),
    recorded_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_needs_external_id UNIQUE (external_id),
    CONSTRAINT ck_needs_recruitment_traceable CHECK (
        recruitment_status IS NULL
        OR (recruitment_source IS NOT NULL AND recruitment_observed_at IS NOT NULL)
    )
);

CREATE INDEX ix_needs_status ON demand.needs (status);
