create table if not exists message_exchange_integration_config (
    id bigint not null auto_increment,
    municipality_id varchar(8) not null,
    namespace varchar(32) not null,
    trigger_status_change_on varchar(255),
    status_change_to varchar(255),
    created datetime(6),
    modified datetime(6),
    primary key (id),
    constraint uq_mex_integration_config_namespace_municipality_id unique (namespace, municipality_id)
) engine=InnoDB;

create index idx_mex_integration_config_namespace_municipality_id
    on message_exchange_integration_config (namespace, municipality_id);
