create table json_parameter (
    json_parameter_order integer default 0 not null,
    errand_id varchar(255) not null,
    id varchar(255) not null,
    parameters_key varchar(255),
    schema_id varchar(255),
    value longtext,
    primary key (id)
) engine=InnoDB;

create index idx_json_parameter_errand_id on json_parameter (errand_id);
create index idx_json_parameter_key on json_parameter (parameters_key);

alter table json_parameter
    add constraint fk_json_parameter_errand_id
    foreign key (errand_id) references errand (id);
