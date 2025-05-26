create table if not exists conversation (
    municipality_id varchar(4) not null,
    latest_synced_sequence_number bigint,
    namespace varchar(32) not null,
    type varchar(32) not null,
    errand_id varchar(36) not null,
    id varchar(36) not null,
    message_exchange_id varchar(36) not null,
    topic varchar(255),
    primary key (id)
) engine=InnoDB;

create table if not exists conversation_relation_id (
    conversation_id varchar(36) not null,
    relation_id varchar(36)
) engine=InnoDB;

create index if not exists idx_message_exchange_id
   on conversation (message_exchange_id);

create index if not exists idx_municipality_id_namespace_errand_id
	on conversation (municipality_id, namespace, errand_id);

alter table if exists conversation_relation_id
	add constraint fk_conversation_relation_conversation_id
	foreign key (conversation_id)
	references conversation (id);
