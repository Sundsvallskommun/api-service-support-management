create table parameter
(
    id              varchar(255) not null,
    name            varchar(255) not null,
    value           varchar(255) not null,
    errand_id       varchar(255) not null,
    primary key (id)
) engine = InnoDB;

alter table if exists parameter
    add constraint fk_parameter_errand_id
        foreign key (errand_id)
            references errand (id);
