create table if not exists handover_idempotency (
    id                     varchar(36)  not null,
    source_errand_id       varchar(36)  not null,
    new_errand_id          varchar(36),
    new_errand_number      varchar(255),
    target_namespace       varchar(255) not null,
    target_municipality_id varchar(16)  not null,
    relation_id            varchar(255),
    warnings               longtext,
    primary key (id),
    unique key uq_handover_source_target (source_errand_id, target_namespace, target_municipality_id)
);
