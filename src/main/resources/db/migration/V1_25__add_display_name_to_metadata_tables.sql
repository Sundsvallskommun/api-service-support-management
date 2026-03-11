alter table contact_reason add column if not exists display_name varchar(255);
alter table external_id_type add column if not exists display_name varchar(255);
alter table status add column if not exists display_name varchar(255);
alter table status add column if not exists external_display_name varchar(255);
alter table parameter drop column if exists parameter_order;
alter table json_parameter drop column if exists json_parameter_order;
