create table if not exists message_exchange_sync (
	active bit,
	municipality_id varchar(4) not null,
	id bigint not null auto_increment,
	latest_synced_sequence_number bigint default 0,
	updated datetime(6),
	namespace varchar(32) not null,
	primary key (id)
) engine=InnoDB;
