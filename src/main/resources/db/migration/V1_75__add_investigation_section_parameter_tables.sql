create table if not exists investigation_section_parameter (
    id                       varchar(255) not null,
    investigation_section_id varchar(255) not null,
    parameters_key           varchar(255) not null,
    display_name             varchar(255),
    parameter_group          varchar(255),
    primary key (id),
    constraint fk_investigation_section_parameter_section_id
        foreign key (investigation_section_id) references investigation_section (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_investigation_section_parameter_section_id
    on investigation_section_parameter (investigation_section_id);

create table if not exists investigation_section_parameter_values (
    investigation_section_parameter_id varchar(255)  not null,
    value_order                        integer       default 0 not null,
    value                              varchar(3000),
    primary key (investigation_section_parameter_id, value_order),
    constraint fk_investigation_section_parameter_values_parameter_id
        foreign key (investigation_section_parameter_id) references investigation_section_parameter (id)
        on delete cascade
) engine=InnoDB;
