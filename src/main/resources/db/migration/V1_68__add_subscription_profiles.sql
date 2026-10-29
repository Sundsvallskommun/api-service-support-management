-- ============================================================================
-- Subscription profiles: a named set of event filters plus the channels the
-- events they match are delivered on. A subscription may point at a profile,
-- in which case the profile alone governs what it delivers and where.
-- ============================================================================

create table if not exists subscription_profile (
    id varchar(255) not null,
    municipality_id varchar(8) not null,
    namespace varchar(32) not null,
    name varchar(255) not null,
    description varchar(255),
    created datetime(6),
    modified datetime(6),
    primary key (id)
) engine=InnoDB;

create index if not exists idx_subscription_profile_municipality_id_namespace
    on subscription_profile (municipality_id, namespace);

alter table if exists subscription_profile
    add constraint uq_subscription_profile_municipality_namespace_name
    unique (municipality_id, namespace, name);

create table if not exists subscription_profile_event_filter (
    profile_id varchar(255) not null,
    type varchar(64) not null,
    subtype varchar(64),
    sort_order integer not null,
    primary key (profile_id, sort_order)
) engine=InnoDB;

alter table if exists subscription_profile_event_filter
    add constraint fk_subscription_profile_event_filter_profile_id
    foreign key (profile_id) references subscription_profile (id)
    on delete cascade;

create table if not exists subscription_profile_channel (
    profile_id varchar(255) not null,
    type varchar(32) not null,
    sort_order integer not null,
    primary key (profile_id, sort_order)
) engine=InnoDB;

alter table if exists subscription_profile_channel
    add constraint fk_subscription_profile_channel_profile_id
    foreign key (profile_id) references subscription_profile (id)
    on delete cascade;

-- ============================================================================
-- Subscriptions may point at a profile. Removing a profile removes the
-- subscriptions to it, as they are meaningless without it.
-- ============================================================================

alter table subscription
    add column if not exists profile_id varchar(255);

create index if not exists idx_subscription_profile_id
    on subscription (profile_id);

alter table if exists subscription
    add constraint fk_subscription_profile_id
    foreign key (profile_id) references subscription_profile (id)
    on delete cascade;

-- ============================================================================
-- Widen the uniqueness of V1_36 so a subscriber may hold one subscription per
-- profile alongside its own. As in V1_36, MariaDB treats NULL as not-equal in
-- UNIQUE indexes, hence a virtual column substituting a sentinel for a NULL
-- profile_id. Real profile ids are UUIDs, so the sentinel can never collide.
-- ============================================================================

alter table subscription
    drop index if exists uq_subscription_subscriber_target_errand;

alter table subscription
    add column profile_key varchar(255)
        as (coalesce(profile_id, '__none__')) virtual;

alter table subscription
    add constraint uq_subscription_subscriber_target_errand_profile
    unique (subscriber_id, target_type, errand_or_namespace_key, profile_key);
