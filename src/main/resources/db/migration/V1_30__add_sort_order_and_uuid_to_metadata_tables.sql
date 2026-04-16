-- Add sort_order column to metadata tables
alter table category add column if not exists sort_order int;
alter table status add column if not exists sort_order int;
alter table role add column if not exists sort_order int;
alter table contact_reason add column if not exists sort_order int;
alter table external_id_type add column if not exists sort_order int;

-- Migrate category PK from bigint to UUID
alter table category add column if not exists uuid_id varchar(255);
update category set uuid_id = uuid() where uuid_id is null;

-- Update type FK to use new UUID
alter table `type` add column if not exists category_uuid_id varchar(255);
update `type` t inner join category c on t.category_id = c.id set t.category_uuid_id = c.uuid_id;

alter table `type` drop foreign key fk_category_id;
alter table `type` drop index uq_category_id_name;
alter table `type` drop column category_id;
alter table `type` change column category_uuid_id category_id varchar(255) not null;
alter table `type` add constraint uq_category_id_name unique (category_id, name);

alter table category modify id bigint not null;
alter table category drop primary key;
alter table category drop column id;
alter table category change column uuid_id id varchar(255) not null;
alter table category add primary key (id);

alter table `type` add constraint fk_category_id foreign key (category_id) references category (id);

-- Migrate contact_reason PK from bigint to UUID
alter table contact_reason add column if not exists uuid_id varchar(255);
update contact_reason set uuid_id = uuid() where uuid_id is null;

-- Update errand FK to use new UUID
alter table errand add column if not exists contact_reason_uuid_id varchar(255);
update errand e inner join contact_reason cr on e.contact_reason_id = cr.id set e.contact_reason_uuid_id = cr.uuid_id;

set @fk_name = (select constraint_name from information_schema.key_column_usage where table_name = 'errand' and column_name = 'contact_reason_id' and referenced_table_name = 'contact_reason' limit 1);
set @sql = concat('alter table errand drop foreign key ', @fk_name);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;
alter table errand drop column contact_reason_id;
alter table errand change column contact_reason_uuid_id contact_reason_id varchar(255);

alter table contact_reason modify id bigint not null;
alter table contact_reason drop primary key;
alter table contact_reason drop column id;
alter table contact_reason change column uuid_id id varchar(255) not null;
alter table contact_reason add primary key (id);

alter table errand add constraint fk_errand_contact_reason_id foreign key (contact_reason_id) references contact_reason (id);

-- Migrate status PK from bigint to UUID
alter table status add column if not exists uuid_id varchar(255);
update status set uuid_id = uuid() where uuid_id is null;
alter table status modify id bigint not null;
alter table status drop primary key;
alter table status drop column id;
alter table status change column uuid_id id varchar(255) not null;
alter table status add primary key (id);

-- Migrate role PK from bigint to UUID
alter table role add column if not exists uuid_id varchar(255);
update role set uuid_id = uuid() where uuid_id is null;
alter table role modify id bigint not null;
alter table role drop primary key;
alter table role drop column id;
alter table role change column uuid_id id varchar(255) not null;
alter table role add primary key (id);

-- Migrate external_id_type PK from bigint to UUID
alter table external_id_type add column if not exists uuid_id varchar(255);
update external_id_type set uuid_id = uuid() where uuid_id is null;
alter table external_id_type modify id bigint not null;
alter table external_id_type drop primary key;
alter table external_id_type drop column id;
alter table external_id_type change column uuid_id id varchar(255) not null;
alter table external_id_type add primary key (id);
