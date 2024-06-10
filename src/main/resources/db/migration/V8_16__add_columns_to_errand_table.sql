alter table errand add column channel varchar(255) after description;
alter table errand add column contact_reason_description varchar(255) after contact_reason_id;