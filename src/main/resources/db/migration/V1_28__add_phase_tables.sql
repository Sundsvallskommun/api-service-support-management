create table if not exists phase (
    id varchar(255) not null,
    municipality_id varchar(8) not null,
    namespace varchar(32) not null,
    name varchar(255) not null,
    display_name varchar(255),
    description varchar(255),
    phase_order integer,
    created datetime(6),
    modified datetime(6),
    primary key (id)
) engine=InnoDB;

create index if not exists idx_phase_municipality_id_namespace
    on phase (municipality_id, namespace);

alter table phase
    add constraint uq_phase_namespace_municipality_id_name
    unique (namespace, municipality_id, name);

create table if not exists phase_allowed_status (
    phase_id varchar(255) not null,
    status varchar(64),
    status_order integer default 0 not null,
    primary key (phase_id, status_order)
) engine=InnoDB;

create index if not exists idx_phase_allowed_status_phase_id
    on phase_allowed_status (phase_id);

alter table if exists phase_allowed_status
    add constraint fk_phase_allowed_status_phase_id
    foreign key (phase_id) references phase (id)
    on delete cascade;

create table if not exists phase_transition (
    id varchar(255) not null,
    phase_id varchar(255) not null,
    target_phase_id varchar(255) not null,
    description varchar(255),
    primary key (id)
) engine=InnoDB;

create index if not exists idx_phase_transition_phase_id
    on phase_transition (phase_id);

alter table if exists phase_transition
    add constraint fk_phase_transition_phase_id
    foreign key (phase_id) references phase (id)
    on delete cascade;

alter table if exists phase_transition
    add constraint fk_phase_transition_target_phase_id
    foreign key (target_phase_id) references phase (id)
    on delete cascade;

create table if not exists errand_phase (
    id varchar(255) not null,
    errand_id varchar(255) not null,
    phase_id varchar(255) not null,
    started datetime(6),
    ended datetime(6),
    primary key (id)
) engine=InnoDB;

create index if not exists idx_errand_phase_errand_id
    on errand_phase (errand_id);

create index if not exists idx_errand_phase_phase_id
    on errand_phase (phase_id);

alter table if exists errand_phase
    add constraint fk_errand_phase_errand_id
    foreign key (errand_id) references errand (id)
    on delete cascade;

alter table if exists errand_phase
    add constraint fk_errand_phase_phase_id
    foreign key (phase_id) references phase (id);
