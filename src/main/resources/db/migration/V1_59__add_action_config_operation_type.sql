create table action_config_operation_type (
    action_config_id varchar(255) not null,
    operation_type varchar(32) not null,
    primary key (action_config_id, operation_type)
) engine=InnoDB;

alter table action_config_operation_type
    add constraint fk_action_config_operation_type_action_config_id
    foreign key (action_config_id) references action_config (id);
