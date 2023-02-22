
alter table errand
	add column municipality_id varchar(255) not null default "2281";

alter table errand 
	alter column municipality_id drop default;

create index idx_errand_municipality_id on errand (municipality_id);
	