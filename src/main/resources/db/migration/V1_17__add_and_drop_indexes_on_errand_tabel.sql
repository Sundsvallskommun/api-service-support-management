create index if not exists idx_errand_municipality_id_namespace_status_created
    on errand (municipality_id,namespace,status,created);

create index if not exists idx_errand_municipality_id_namespace_status_touched
    on errand (municipality_id,namespace,status,touched);

drop index if exists idx_errand_municipality_id_namespace_created on errand;
drop index if exists idx_errand_municipality_id_namespace_touched on errand;

