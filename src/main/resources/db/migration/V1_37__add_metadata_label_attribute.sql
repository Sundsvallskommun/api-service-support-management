create table metadata_label_attribute (
    metadata_label_id varchar(255) not null,
    `key` varchar(255) not null,
    `value` text not null
) engine=InnoDB;

create index idx_metadata_label_attribute_label_id_key
   on metadata_label_attribute (metadata_label_id, `key`);

alter table metadata_label_attribute
   add constraint uk_metadata_label_attribute_label_id_key
   unique (metadata_label_id, `key`);

alter table metadata_label_attribute
   add constraint fk_metadata_label_attribute_metadata_label
   foreign key (metadata_label_id)
   references metadata_label (id);
