create table stakeholder_metadata
(
    stakeholder_id bigint       not null,
    metadata       varchar(255),
    metadata_key   varchar(255) not null,
    primary key (stakeholder_id, metadata_key)
) engine = InnoDB;


alter table if exists stakeholder_metadata
    add constraint fk_stakeholder_metadata_stakeholder_id
        foreign key (stakeholder_id)
            references stakeholder (id);
