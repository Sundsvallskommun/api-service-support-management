create table metadata_label_classification (
    created datetime(6),
    modified datetime(6),
    municipality_id varchar(8) not null,
    namespace varchar(32) not null,
    classification varchar(255) not null,
    display_name varchar(255),
    id varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create index idx_label_classification_ns_municipality_id
    on metadata_label_classification (namespace, municipality_id);

alter table if exists metadata_label_classification
    add constraint uq_label_classification_ns_municipality_classification unique (namespace, municipality_id, classification);
