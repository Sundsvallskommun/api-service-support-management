alter table attachment
    add column namespace varchar(255);

alter table attachment
    add column municipality_id varchar(255);

alter table communication
    add column namespace varchar(255);

alter table communication
    add column municipality_id varchar(255);

alter table communication_attachment
    add column namespace varchar(255);

alter table communication_attachment
    add column municipality_id varchar(255);

alter table revision
    add column namespace varchar(255);

alter table revision
    add column municipality_id varchar(255);

create index idx_attachment_municipality_id
    on attachment (municipality_id);

create index idx_attachment_namespace
    on attachment (namespace);

create index idx_communication_municipality_id
    on communication (municipality_id);

create index idx_communication_namespace
    on communication (namespace);

create index idx_communication_attachment_municipality_id
    on communication_attachment (municipality_id);

create index idx_communication_attachment_namespace
    on communication_attachment (namespace);


create index revision_municipality_id_index
    on revision (municipality_id);

create index revision_namespace_index
    on revision (namespace);
