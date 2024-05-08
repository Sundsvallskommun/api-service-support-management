create table email_worker_config (
        days_of_inactivity_before_reject integer,
        enabled bit not null,
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        errand_closed_email_sender varchar(255),
        errand_closed_email_template varchar(5000),
        inactive_status varchar(255),
        municipality_id varchar(255) not null,
        namespace varchar(255) not null,
        status_change_to varchar(255),
        status_for_new varchar(255),
        trigger_status_change_on varchar(255),
        primary key (id)
    ) engine=InnoDB;

create index idx_namespace_municipality_id
   on email_worker_config (namespace, municipality_id);

alter table if exists email_worker_config
   add constraint uq_namespace_municipality_id unique (namespace, municipality_id);