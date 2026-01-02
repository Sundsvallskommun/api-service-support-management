-- Set config property for if notification to reporter should be triggered or not when creating internal message to the stakeholder to false for all existing configs
insert into namespace_config_value (namespace_config_id, `key`, `value`, `type`)
select id, 'NOTIFY_REPORTER', 'false', 'BOOLEAN' from namespace_config;

