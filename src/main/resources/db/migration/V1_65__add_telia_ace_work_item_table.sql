create table telia_ace_work_item (
    id                    varchar(36)  not null,
    errand_id             varchar(36)  not null,
    municipality_id       varchar(8)   not null,
    namespace             varchar(32)  not null,
    from_address          varchar(255),
    subject               varchar(255),
    content_url           varchar(255),
    predefined_agent_name varchar(255),
    created               datetime(3)  not null,
    primary key (id)
) engine=InnoDB;

create index idx_telia_ace_work_item_errand_id
    on telia_ace_work_item (errand_id);
