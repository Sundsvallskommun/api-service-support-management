create index if not exists idx_contact_channel_type_value 
    on contact_channel (type, value);

create index if not exists idx_contact_channel_value 
    on contact_channel (value);
       
create index if not exists idx_errand_id_metadata_label_id 
    on errand_labels (errand_id, metadata_label_id);
       
create index if not exists idx_resource_path 
    on metadata_label (resource_path);