create table web_message_collect (
    id bigint not null auto_increment,
    instance varchar(255) not null,
    municipality_id varchar(255) not null,
    namespace varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table web_message_collect_family_ids (
    web_message_collect_id bigint not null,
    family_id varchar(255)
) engine=InnoDB;

alter table if exists web_message_collect
	add constraint uq_namespace_municipality_id_instance_family_id unique (namespace, municipality_id, instance);

alter table if exists web_message_collect_family_ids
	add constraint fk_web_message_collect_family_ids_web_message_collect_id
	foreign key (web_message_collect_id)
	references web_message_collect (id);
