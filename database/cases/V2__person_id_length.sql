-- La persona de un caso puede ser un usuario de Jira sin matrícula (se identifica por su correo).
alter table cases.case_file alter column person_id type varchar(120);
