-- Create config value table
create table namespace_config_value (
    namespace_config_id bigint not null,
    `key` varchar(255) not null,
    `value` text not null,
    `type` enum ('BOOLEAN','INTEGER','STRING') not null
) engine=InnoDB;

create index idx_namespace_config_value_namespace_config_id_key 
   on namespace_config_value (namespace_config_id, `key`);

alter table if exists namespace_config_value 
   add constraint uk_namespace_config_id_key_value unique (namespace_config_id, `key`, `value`);

alter table if exists namespace_config_value 
   add constraint fk_namespace_config_value_namespace_config 
   foreign key (namespace_config_id) 
   references namespace_config (id);
       
-- Add config settings in namespace_config table as values in namespace_config_value table
insert into namespace_config_value (namespace_config_id, `key`, `value`, `type`)
select id, 'DISPLAY_NAME', display_name, 'STRING' from namespace_config;

insert into namespace_config_value (namespace_config_id, `key`, `value`, `type`)
select id, 'SHORT_CODE', short_code, 'STRING' from namespace_config;

insert into namespace_config_value (namespace_config_id, `key`, `value`, `type`)
select id, 'NOTIFICATION_TTL_IN_DAYS', notification_ttl_in_days, 'INTEGER' from namespace_config;

insert into namespace_config_value (namespace_config_id, `key`, `value`, `type`)
select id, 'ACCESS_CONTROL', case when access_control = 0 then 'false' else 'true' end, 'BOOLEAN' from namespace_config;

-- Remove columns with configuration settings from namespace_config table
alter table if exists namespace_config drop column if exists display_name;
alter table if exists namespace_config drop column if exists short_code;
alter table if exists namespace_config drop column if exists notification_ttl_in_days;
alter table if exists namespace_config drop column if exists access_control;

