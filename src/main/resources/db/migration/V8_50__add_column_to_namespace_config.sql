alter table if exists namespace_config
    add column if not exists notification_ttl_in_days integer not null;
    
    
update namespace_config set notification_ttl_in_days = 40;