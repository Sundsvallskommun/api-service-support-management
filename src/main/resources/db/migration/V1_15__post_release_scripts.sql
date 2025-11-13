alter table if exists errand_labels 
   add constraint fk_errand_labels_metadata_label_id 
   foreign key (metadata_label_id) 
   references metadata_label (id);
   
drop index idx_errand_labels_errand_id_label on errand_labels;

alter table if exists errand_labels drop column if exists label;

alter table if exists metadata_label drop column if exists name;

drop table if exists label;