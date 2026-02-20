    create table if not exists errand_access_labels (
        errand_id varchar(255) not null,
        metadata_label_id varchar(255) not null
    ) engine=InnoDB;

    create index if not exists idx_errand_access_labels_errand_id_metadata_label_id
        on errand_access_labels (errand_id, metadata_label_id);

    create index if not exists idx_errand_access_labels_metadata_label_id_errand_id
        on errand_access_labels (metadata_label_id, errand_id);

    create index if not exists idx_errand_access_labels_errand_id
        on errand_access_labels (errand_id);

    create index if not exists idx_errand_access_labels_metadata_label_id
        on errand_access_labels (metadata_label_id);

    alter table if exists errand_access_labels
        add constraint fk_errand_access_labels_metadata_label_id
        foreign key (metadata_label_id)
        references metadata_label (id);

    alter table if exists errand_access_labels
        add constraint fk_errand_access_labels_errand_id
        foreign key (errand_id)
        references errand (id);

    -- Backfill: insert leaf labels for all existing errands.
    -- A label is a "leaf" on an errand if no other label on that errand
    -- is a descendant of it (i.e., has a resource_path that starts with this label's resource_path + '/').
    insert into errand_access_labels (errand_id, metadata_label_id)
    select el.errand_id, el.metadata_label_id
    from errand_labels el
    join metadata_label ml_self on ml_self.id = el.metadata_label_id
    where not exists (
        select 1
        from errand_labels el2
        join metadata_label ml_other on ml_other.id = el2.metadata_label_id
        where el2.errand_id = el.errand_id
          and ml_other.resource_path like concat(ml_self.resource_path, '/%')
    );
