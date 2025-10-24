alter table if exists errand_labels
    add column if not exists metadata_label_id varchar(255);
    
create index if not exists idx_errand_id 
    on errand_labels (errand_id);

create index if not exists  idx_metadata_label_id 
    on errand_labels (metadata_label_id);
