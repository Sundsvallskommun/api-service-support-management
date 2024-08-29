create table errand_labels
(
    errand_id varchar(255) not null,
    label     varchar(255)
) engine = InnoDB;



alter table if exists errand_labels
    add constraint fk_errand_labels_errand_id
        foreign key (errand_id)
            references errand (id);
