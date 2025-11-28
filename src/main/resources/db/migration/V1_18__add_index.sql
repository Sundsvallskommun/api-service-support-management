-- These were dropped in script v1.17, but it turns out we need them
create index if not exists idx_errand_municipality_id_namespace_created
    on errand (municipality_id,namespace,created);

create index if not exists idx_errand_municipality_id_namespace_touched
    on errand (municipality_id,namespace,touched);

-- Add index where first sort is on metadata_label_id
create index if not exists idx_metadata_label_id_errand_id
    on errand_labels (metadata_label_id,errand_id);
