create table if not exists statement (
    id                            varchar(255) not null,
    errand_id                     varchar(255) not null,
    municipality_id               varchar(8)   not null,
    namespace                     varchar(32)  not null,
    type                          varchar(128),
    status                        varchar(32)  not null,
    title                         varchar(255),
    description                   longtext,
    due_at                        datetime(6),
    completed_at                  datetime(6),
    created_by                    varchar(255),
    modified_by                   varchar(255),
    created                       datetime(6),
    modified                      datetime(6),
    version                       bigint default 0 not null,
    counterparty_name             varchar(255) not null,
    counterparty_external_id      varchar(255),
    counterparty_external_id_type varchar(128),
    counterparty_reference        varchar(128),
    question                      longtext,
    sent_at                       datetime(6),
    reminded_at                   datetime(6),
    responded_at                  datetime(6),
    outcome                       varchar(32),
    response_text                 longtext,
    communication_id              varchar(36),
    primary key (id),
    constraint fk_statement_errand_id
        foreign key (errand_id) references errand (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_statement_errand_id
    on statement (errand_id);

create index if not exists idx_statement_ns_status
    on statement (municipality_id, namespace, status);

create index if not exists idx_statement_due_at
    on statement (due_at);

create index if not exists idx_statement_counterparty_external_id
    on statement (counterparty_external_id);

create table if not exists investigation (
    id                        varchar(255) not null,
    errand_id                 varchar(255) not null,
    municipality_id           varchar(8)   not null,
    namespace                 varchar(32)  not null,
    type                      varchar(128),
    status                    varchar(32)  not null,
    title                     varchar(255),
    description               longtext,
    due_at                    datetime(6),
    completed_at              datetime(6),
    created_by                varchar(255),
    modified_by               varchar(255),
    created                   datetime(6),
    modified                  datetime(6),
    version                   bigint default 0 not null,
    investigator_user_id      varchar(255),
    started_at                datetime(6),
    summary                   longtext,
    conclusion                longtext,
    recommendation            varchar(32),
    recommendation_motivation longtext,
    primary key (id),
    constraint fk_investigation_errand_id
        foreign key (errand_id) references errand (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_investigation_errand_id
    on investigation (errand_id);

create index if not exists idx_investigation_ns_status
    on investigation (municipality_id, namespace, status);

create index if not exists idx_investigation_due_at
    on investigation (due_at);

create table if not exists investigation_section (
    id               varchar(255) not null,
    investigation_id varchar(255) not null,
    section_key      varchar(64)  not null,
    heading          varchar(255),
    sort_order       integer,
    assessment       varchar(32)  not null,
    text             longtext,
    completed_by     varchar(255),
    completed_at     datetime(6),
    primary key (id),
    constraint uq_investigation_section_investigation_id_section_key
        unique (investigation_id, section_key),
    constraint fk_investigation_section_investigation_id
        foreign key (investigation_id) references investigation (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_investigation_section_investigation_id
    on investigation_section (investigation_id);

create table if not exists decision (
    id                   varchar(255) not null,
    errand_id            varchar(255) not null,
    municipality_id      varchar(8)   not null,
    namespace            varchar(32)  not null,
    type                 varchar(128),
    status               varchar(32)  not null,
    title                varchar(255),
    description          longtext,
    due_at               datetime(6),
    completed_at         datetime(6),
    created_by           varchar(255),
    modified_by          varchar(255),
    created              datetime(6),
    modified             datetime(6),
    version              bigint default 0 not null,
    outcome              varchar(32)  not null,
    method               varchar(16)  not null,
    decided_by           varchar(255) not null,
    decided_by_role      varchar(128),
    decided_at           datetime(6)  not null,
    legal_basis          varchar(255),
    delegation_reference varchar(64),
    justification        longtext,
    appealable           bit,
    valid_from           date,
    valid_to             date,
    investigation_id     varchar(255),
    errand_process_id    varchar(36),
    primary key (id),
    constraint fk_decision_errand_id
        foreign key (errand_id) references errand (id)
        on delete cascade,
    constraint fk_decision_investigation_id
        foreign key (investigation_id) references investigation (id)
        on delete set null
) engine=InnoDB;

create index if not exists idx_decision_errand_id
    on decision (errand_id);

create index if not exists idx_decision_ns_outcome
    on decision (municipality_id, namespace, outcome);

create index if not exists idx_decision_valid_to
    on decision (valid_to);

create index if not exists idx_decision_due_at
    on decision (due_at);

create table if not exists decision_term (
    id          varchar(255) not null,
    decision_id varchar(255) not null,
    sort_order  integer,
    category    varchar(128),
    text        longtext     not null,
    primary key (id),
    constraint fk_decision_term_decision_id
        foreign key (decision_id) references decision (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_decision_term_decision_id
    on decision_term (decision_id);

alter table if exists measure
    add column if not exists municipality_id varchar(8),
    add column if not exists namespace       varchar(32),
    add column if not exists status          varchar(32),
    add column if not exists title           varchar(255),
    add column if not exists due_at          datetime(6),
    add column if not exists completed_at    datetime(6),
    add column if not exists created_by      varchar(255),
    add column if not exists modified_by     varchar(255),
    add column if not exists version         bigint default 0 not null,
    add column if not exists result          varchar(32),
    add column if not exists result_text     longtext,
    add column if not exists decision_id     varchar(255),
    add column if not exists statement_id    varchar(255);

update measure m
    join errand e on e.id = m.errand_id
    set m.municipality_id = e.municipality_id,
        m.namespace       = e.namespace
    where m.municipality_id is null
       or m.namespace is null;

update measure
    set status       = case when executed is not null then 'COMPLETED' else 'ACTIVE' end,
        completed_at = executed
    where status is null;

alter table if exists measure
    modify municipality_id varchar(8)  not null,
    modify namespace       varchar(32) not null,
    modify status          varchar(32) not null;

alter table if exists measure
    drop foreign key if exists fk_measure_errand_id;

alter table if exists measure
    add constraint fk_measure_errand_id
    foreign key (errand_id) references errand (id)
    on delete cascade;

create index if not exists idx_measure_ns_status
    on measure (municipality_id, namespace, status);

create index if not exists idx_measure_due_at
    on measure (due_at);

alter table if exists measure
    add constraint fk_measure_decision_id
    foreign key if not exists (decision_id) references decision (id)
    on delete set null;

alter table if exists measure
    add constraint fk_measure_statement_id
    foreign key if not exists (statement_id) references statement (id)
    on delete set null;

create table if not exists statement_attachment (
    id            varchar(255) not null,
    statement_id  varchar(255) not null,
    attachment_id varchar(255) not null,
    sort_order    integer,
    created       datetime(6),
    created_by    varchar(255),
    primary key (id),
    constraint uq_statement_attachment_statement_id_attachment_id
        unique (statement_id, attachment_id),
    constraint fk_statement_attachment_statement_id
        foreign key (statement_id) references statement (id)
        on delete cascade,
    constraint fk_statement_attachment_attachment_id
        foreign key (attachment_id) references attachment (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_statement_attachment_statement_id
    on statement_attachment (statement_id);

create index if not exists idx_statement_attachment_attachment_id
    on statement_attachment (attachment_id);

create table if not exists investigation_attachment (
    id               varchar(255) not null,
    investigation_id varchar(255) not null,
    attachment_id    varchar(255) not null,
    sort_order       integer,
    created          datetime(6),
    created_by       varchar(255),
    primary key (id),
    constraint uq_investigation_attachment_investigation_id_attachment_id
        unique (investigation_id, attachment_id),
    constraint fk_investigation_attachment_investigation_id
        foreign key (investigation_id) references investigation (id)
        on delete cascade,
    constraint fk_investigation_attachment_attachment_id
        foreign key (attachment_id) references attachment (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_investigation_attachment_investigation_id
    on investigation_attachment (investigation_id);

create index if not exists idx_investigation_attachment_attachment_id
    on investigation_attachment (attachment_id);

create table if not exists decision_attachment (
    id            varchar(255) not null,
    decision_id   varchar(255) not null,
    attachment_id varchar(255) not null,
    sort_order    integer,
    created       datetime(6),
    created_by    varchar(255),
    primary key (id),
    constraint uq_decision_attachment_decision_id_attachment_id
        unique (decision_id, attachment_id),
    constraint fk_decision_attachment_decision_id
        foreign key (decision_id) references decision (id)
        on delete cascade,
    constraint fk_decision_attachment_attachment_id
        foreign key (attachment_id) references attachment (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_decision_attachment_decision_id
    on decision_attachment (decision_id);

create index if not exists idx_decision_attachment_attachment_id
    on decision_attachment (attachment_id);

create table if not exists measure_attachment (
    id            varchar(255) not null,
    measure_id    varchar(255) not null,
    attachment_id varchar(255) not null,
    sort_order    integer,
    created       datetime(6),
    created_by    varchar(255),
    primary key (id),
    constraint uq_measure_attachment_measure_id_attachment_id
        unique (measure_id, attachment_id),
    constraint fk_measure_attachment_measure_id
        foreign key (measure_id) references measure (id)
        on delete cascade,
    constraint fk_measure_attachment_attachment_id
        foreign key (attachment_id) references attachment (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_measure_attachment_measure_id
    on measure_attachment (measure_id);

create index if not exists idx_measure_attachment_attachment_id
    on measure_attachment (attachment_id);

create table if not exists statement_json_parameter (
    id                varchar(255) not null,
    statement_id      varchar(255) not null,
    json_parameter_id varchar(255) not null,
    primary key (id),
    constraint uq_statement_json_parameter_statement_id_json_parameter_id
        unique (statement_id, json_parameter_id),
    constraint uq_statement_json_parameter_json_parameter_id
        unique (json_parameter_id),
    constraint fk_statement_json_parameter_statement_id
        foreign key (statement_id) references statement (id)
        on delete cascade,
    constraint fk_statement_json_parameter_json_parameter_id
        foreign key (json_parameter_id) references json_parameter (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_statement_json_parameter_statement_id
    on statement_json_parameter (statement_id);

create index if not exists idx_statement_json_parameter_json_parameter_id
    on statement_json_parameter (json_parameter_id);

create table if not exists investigation_json_parameter (
    id                varchar(255) not null,
    investigation_id  varchar(255) not null,
    json_parameter_id varchar(255) not null,
    primary key (id),
    constraint uq_investigation_json_parameter_investigation_id_parameter_id
        unique (investigation_id, json_parameter_id),
    constraint uq_investigation_json_parameter_json_parameter_id
        unique (json_parameter_id),
    constraint fk_investigation_json_parameter_investigation_id
        foreign key (investigation_id) references investigation (id)
        on delete cascade,
    constraint fk_investigation_json_parameter_json_parameter_id
        foreign key (json_parameter_id) references json_parameter (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_investigation_json_parameter_investigation_id
    on investigation_json_parameter (investigation_id);

create index if not exists idx_investigation_json_parameter_json_parameter_id
    on investigation_json_parameter (json_parameter_id);

create table if not exists investigation_section_json_parameter (
    id                       varchar(255) not null,
    investigation_section_id varchar(255) not null,
    json_parameter_id        varchar(255) not null,
    primary key (id),
    constraint uq_investigation_section_json_parameter_section_parameter
        unique (investigation_section_id, json_parameter_id),
    constraint uq_investigation_section_json_parameter_parameter_id
        unique (json_parameter_id),
    constraint fk_investigation_section_json_parameter_section_id
        foreign key (investigation_section_id) references investigation_section (id)
        on delete cascade,
    constraint fk_investigation_section_json_parameter_parameter_id
        foreign key (json_parameter_id) references json_parameter (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_investigation_section_json_parameter_section_id
    on investigation_section_json_parameter (investigation_section_id);

create index if not exists idx_investigation_section_json_parameter_parameter_id
    on investigation_section_json_parameter (json_parameter_id);

create table if not exists decision_json_parameter (
    id                varchar(255) not null,
    decision_id       varchar(255) not null,
    json_parameter_id varchar(255) not null,
    primary key (id),
    constraint uq_decision_json_parameter_decision_id_json_parameter_id
        unique (decision_id, json_parameter_id),
    constraint uq_decision_json_parameter_json_parameter_id
        unique (json_parameter_id),
    constraint fk_decision_json_parameter_decision_id
        foreign key (decision_id) references decision (id)
        on delete cascade,
    constraint fk_decision_json_parameter_json_parameter_id
        foreign key (json_parameter_id) references json_parameter (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_decision_json_parameter_decision_id
    on decision_json_parameter (decision_id);

create index if not exists idx_decision_json_parameter_json_parameter_id
    on decision_json_parameter (json_parameter_id);

create table if not exists measure_json_parameter (
    id                varchar(255) not null,
    measure_id        varchar(255) not null,
    json_parameter_id varchar(255) not null,
    primary key (id),
    constraint uq_measure_json_parameter_measure_id_json_parameter_id
        unique (measure_id, json_parameter_id),
    constraint uq_measure_json_parameter_json_parameter_id
        unique (json_parameter_id),
    constraint fk_measure_json_parameter_measure_id
        foreign key (measure_id) references measure (id)
        on delete cascade,
    constraint fk_measure_json_parameter_json_parameter_id
        foreign key (json_parameter_id) references json_parameter (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_measure_json_parameter_measure_id
    on measure_json_parameter (measure_id);

create index if not exists idx_measure_json_parameter_json_parameter_id
    on measure_json_parameter (json_parameter_id);

create table if not exists attachment_purpose (
    id              varchar(255) not null,
    name            varchar(255) not null,
    display_name    varchar(255),
    sort_order      integer,
    deprecated      bit          not null,
    municipality_id varchar(8)   not null,
    namespace       varchar(32)  not null,
    created         datetime(6),
    modified        datetime(6),
    primary key (id),
    constraint uq_attachment_purpose_namespace_municipality_id_name
        unique (namespace, municipality_id, name)
) engine=InnoDB;

create index if not exists idx_attachment_purpose_namespace_municipality_id
    on attachment_purpose (namespace, municipality_id);

alter table if exists attachment
    add column if not exists attachment_purpose_id varchar(255);

create index if not exists idx_attachment_attachment_purpose_id
    on attachment (attachment_purpose_id);

alter table if exists attachment
    add constraint fk_attachment_attachment_purpose_id
    foreign key if not exists (attachment_purpose_id) references attachment_purpose (id);
