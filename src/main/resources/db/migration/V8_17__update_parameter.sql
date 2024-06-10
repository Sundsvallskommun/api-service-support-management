drop table parameter;

create table parameter
(
    errand_id      varchar(255) not null,
    id             varchar(255) not null,
    parameters_key varchar(255),
    primary key (id)
) engine = InnoDB;

create table parameter_values
(
    parameter_id varchar(255) not null,
    value        varchar(255)
) engine = InnoDB;


alter table if exists parameter_values
    add constraint fk_parameter_values_parameter_id
        foreign key (parameter_id)
            references parameter (id);

alter table if exists parameter
    add constraint fk_parameter_errand_id
        foreign key (errand_id)
            references errand (id);
