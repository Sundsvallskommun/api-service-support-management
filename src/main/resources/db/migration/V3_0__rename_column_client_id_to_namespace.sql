
drop index if exists idx_errand_client_id_tag on errand;

alter table errand change column client_id_tag namespace varchar(255) not null;

create index idx_errand_namespace on errand (namespace);

delete from tag where type = 'CLIENT_ID';

alter table tag change type type ENUM('CATEGORY', 'STATUS', 'TYPE');
