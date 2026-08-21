-- Resource access control is off for every existing namespace, so which resources a user may reach keeps being decided
-- by their labels alone until the access mapper has resource access configured for the namespace.
insert into namespace_config_value (namespace_config_id, `key`, `value`, `type`)
select id, 'RESOURCE_ACCESS_CONTROL', 'false', 'BOOLEAN' from namespace_config;
