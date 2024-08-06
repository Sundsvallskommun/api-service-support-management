alter table if exists stakeholder
    add column if not exists city varchar(255);
alter table if exists stakeholder
    add column if not exists organization_name varchar(255);