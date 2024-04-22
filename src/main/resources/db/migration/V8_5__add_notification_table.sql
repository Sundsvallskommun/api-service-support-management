create table notification
(
    acknowledged    bit,
    created         datetime(6),
    expires         datetime(6),
    modified        datetime(6),
    content         varchar(255),
    created_by      varchar(255),
    description     varchar(255),
    errand_id       varchar(255),
    id              varchar(255) not null,
    municipality_id varchar(255) not null,
    namespace       varchar(255) not null,
    owner_full_name varchar(255),
    owner_id        varchar(255),
    type            varchar(255),
    primary key (id)
) engine = InnoDB;


create index idx_namespace_municipality_id
    on notification (namespace, municipality_id);
