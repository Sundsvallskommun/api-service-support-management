create table action_config (
    id varchar(255) not null,
    municipality_id varchar(8) not null,
    namespace varchar(32) not null,
    name varchar(255) not null,
    active bit not null default 0,
    display_value varchar(255),
    created datetime(6),
    modified datetime(6),
    primary key (id)
) engine=InnoDB;

create index idx_action_config_municipality_id_namespace
    on action_config (municipality_id, namespace);

create table action_config_condition (
    id varchar(255) not null,
    action_config_id varchar(255) not null,
    condition_key varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_action_config_condition_action_config_id
    on action_config_condition (action_config_id);

alter table action_config_condition
    add constraint fk_action_config_condition_action_config_id
    foreign key (action_config_id) references action_config (id);

create table action_config_condition_values (
    action_config_condition_id varchar(255) not null,
    value varchar(2000),
    value_order integer default 0 not null,
    primary key (action_config_condition_id, value_order)
) engine=InnoDB;

alter table action_config_condition_values
    add constraint fk_action_config_condition_values_condition_id
    foreign key (action_config_condition_id) references action_config_condition (id);

create table action_config_parameter (
    id varchar(255) not null,
    action_config_id varchar(255) not null,
    parameter_key varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_action_config_parameter_action_config_id
    on action_config_parameter (action_config_id);

alter table action_config_parameter
    add constraint fk_action_config_parameter_action_config_id
    foreign key (action_config_id) references action_config (id);

create table action_config_parameter_values (
    action_config_parameter_id varchar(255) not null,
    value varchar(2000),
    value_order integer default 0 not null,
    primary key (action_config_parameter_id, value_order)
) engine=InnoDB;

alter table action_config_parameter_values
    add constraint fk_action_config_parameter_values_parameter_id
    foreign key (action_config_parameter_id) references action_config_parameter (id);
