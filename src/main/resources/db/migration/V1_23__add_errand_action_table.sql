create table errand_action (
    id varchar(255) not null,
    errand_id varchar(255) not null,
    execute_after datetime(6),
    action_config_id varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_errand_action_errand_id
    on errand_action (errand_id);

create index idx_errand_action_execute_after
    on errand_action (execute_after);

alter table errand_action
    add constraint fk_errand_action_errand_id
    foreign key (errand_id) references errand (id);

alter table errand_action
    add constraint fk_errand_action_action_config_id
    foreign key (action_config_id) references action_config (id);
