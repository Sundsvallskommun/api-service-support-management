create table measure (
    id varchar(255) not null,
    errand_id varchar(255) not null,
    responsible_user varchar(255),
    type varchar(255),
    planned_start datetime(6),
    planned_complete datetime(6),
    executed datetime(6),
    added_by_user varchar(255),
    added_by_role varchar(255),
    goal varchar(255),
    description varchar(1000),
    accept varchar(50),
    accept_motivation varchar(255),
    rework_goal varchar(255),
    rework_description varchar(1000),
    created datetime(6),
    modified datetime(6),
    primary key (id),
    constraint fk_measure_errand_id foreign key (errand_id) references errand(id)
) engine=InnoDB;

create index idx_measure_errand_id on measure(errand_id);