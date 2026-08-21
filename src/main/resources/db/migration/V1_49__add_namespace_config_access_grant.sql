-- Table holding the access a namespace grants. One row per grant, so a scope is extended by adding rows. The scope is
-- either a reserved value resolved per errand by the service (LIMITED, REPORTER) or the name of a role supplied by the
-- access mapper.
create table namespace_config_access_grant (
    namespace_config_id bigint       not null,
    `scope`             varchar(255) not null,
    `type`              varchar(255) not null,
    `value`             text         not null,
    access_level        varchar(255)
) engine=InnoDB;

create index idx_namespace_config_access_grant_namespace_config_id_scope
    on namespace_config_access_grant (namespace_config_id, `scope`);

alter table namespace_config_access_grant
    add constraint uk_namespace_config_id_scope_type_value unique (namespace_config_id, `scope`, `type`, `value`(255));

alter table namespace_config_access_grant
    add constraint fk_namespace_config_access_grant_namespace_config
        foreign key (namespace_config_id) references namespace_config (id);

-- Role based mapping is off for every existing namespace, so nothing changes in how errands are mapped until a
-- namespace opts in.
insert into namespace_config_value (namespace_config_id, `key`, `value`, `type`)
select id, 'ROLE_BASED_MAPPING', 'false', 'BOOLEAN' from namespace_config;
