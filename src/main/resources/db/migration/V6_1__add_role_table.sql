create table role (
	id bigint not null auto_increment,
	created datetime(6),
	modified datetime(6),
	municipality_id varchar(255) not null,
	name varchar(255) not null,
	namespace varchar(255) not null,
	primary key (id)
) engine=InnoDB;

create index idx_namespace_municipality_id on role (namespace, municipality_id);

alter table role
	add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);
