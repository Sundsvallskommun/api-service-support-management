-- ============================================================================
-- A subscriber leaving a subscription profile is remembered here, so the
-- members sync does not subscribe them to it again. Removing either the
-- subscriber or the profile removes the record.
-- ============================================================================

create table if not exists subscription_opt_out (
    id varchar(255) not null,
    subscriber_id varchar(255) not null,
    profile_id varchar(255) not null,
    created datetime(6),
    primary key (id)
) engine=InnoDB;

create index if not exists idx_subscription_opt_out_profile_id
    on subscription_opt_out (profile_id);

alter table if exists subscription_opt_out
    add constraint uq_subscription_opt_out_subscriber_profile
    unique (subscriber_id, profile_id);

alter table if exists subscription_opt_out
    add constraint fk_subscription_opt_out_subscriber_id
    foreign key (subscriber_id) references subscriber (id)
    on delete cascade;

alter table if exists subscription_opt_out
    add constraint fk_subscription_opt_out_profile_id
    foreign key (profile_id) references subscription_profile (id)
    on delete cascade;
