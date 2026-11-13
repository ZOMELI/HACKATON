-- Esquema cases (lo crea Flyway con -schemas=cases). El caso completo se guarda como JSON en payload;
-- las columnas sueltas existen para filtrar y para la unicidad de la clave de deduplicación.
create table cases.case_file (
    id          varchar(40)  primary key,
    dedupe_key  varchar(200) not null unique,
    type        varchar(40)  not null,
    status      varchar(20)  not null,
    person_id   varchar(20)  not null,
    owner_id    varchar(20),
    due_date    date,
    opened_at   timestamp with time zone not null,
    payload     text         not null
);
create index case_file_status_idx on cases.case_file (status, due_date);

create table cases.alert_settings (
    id                    integer primary key,
    contract_expiry_days  integer not null,
    availability_days     integer not null,
    updated_at            timestamp with time zone,
    updated_by            varchar(40)
);
