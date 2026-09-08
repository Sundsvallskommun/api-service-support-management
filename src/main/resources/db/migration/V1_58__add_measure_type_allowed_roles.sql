-- Explicit assignments only. No role grants are inferred from existing measure_group text.
create table measure_type_allowed_role (
    measure_type_id varchar(255) not null,
    role_id varchar(255) not null,
    primary key (measure_type_id, role_id),
    constraint fk_measure_type_allowed_role_type foreign key (measure_type_id) references measure_type(id),
    constraint fk_measure_type_allowed_role_role foreign key (role_id) references role(id)
) engine=InnoDB;

create index idx_measure_type_allowed_role_role on measure_type_allowed_role(role_id);
