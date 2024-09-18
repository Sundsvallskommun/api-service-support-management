alter table namespace_config
	add column display_name varchar(255);
	
update namespace_config
	set display_name = CONCAT(UCASE(LEFT(namespace, 1)), LCASE(SUBSTRING(namespace, 2)));
	
alter table namespace_config
	modify display_name varchar(255) not null;
	
create index idx_municipality_id on namespace_config (municipality_id);
