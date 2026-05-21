-- ============================================================================
-- Subscriber and its child collections
-- ============================================================================

create table if not exists subscriber (
    id varchar(255) not null,
    municipality_id varchar(8) not null,
    namespace varchar(32) not null,
    name varchar(255),
    identifier_type varchar(16) not null,
    identifier_value varchar(255) not null,
    paused_from datetime(6),
    paused_until datetime(6),
    created datetime(6),
    modified datetime(6),
    created_by_type varchar(16),
    created_by_value varchar(255),
    primary key (id)
) engine=InnoDB;

create index if not exists idx_subscriber_municipality_id_namespace
    on subscriber (municipality_id, namespace);

create index if not exists idx_subscriber_municipality_id_namespace_identifier
    on subscriber (municipality_id, namespace, identifier_type, identifier_value);

-- Note: MariaDB treats NULL as not-equal in UNIQUE indexes, so multiple rows with NULL name are still permitted.
-- Service-layer existsBy check covers that null-name case.
alter table if exists subscriber
    add constraint uq_subscriber_municipality_namespace_identifier_name
    unique (municipality_id, namespace, identifier_type, identifier_value, name);

create table if not exists subscriber_channel (
    subscriber_id varchar(255) not null,
    type varchar(32) not null,
    destination varchar(255),
    sort_order integer not null,
    primary key (subscriber_id, sort_order)
) engine=InnoDB;

alter table if exists subscriber_channel
    add constraint fk_subscriber_channel_subscriber_id
    foreign key (subscriber_id) references subscriber (id)
    on delete cascade;

create table if not exists subscriber_event_filter (
    subscriber_id varchar(255) not null,
    type varchar(64) not null,
    subtype varchar(64),
    sort_order integer not null,
    primary key (subscriber_id, sort_order)
) engine=InnoDB;

alter table if exists subscriber_event_filter
    add constraint fk_subscriber_event_filter_subscriber_id
    foreign key (subscriber_id) references subscriber (id)
    on delete cascade;

-- ============================================================================
-- Subscription and its child collections.
-- FKs both use ON DELETE CASCADE: deleting either the parent subscriber or
-- the target errand also removes the subscription. Cascade does not propagate
-- upwards — deleting a subscription leaves subscriber and errand untouched.
-- ============================================================================

create table if not exists subscription (
    id varchar(255) not null,
    subscriber_id varchar(255) not null,
    target_type varchar(16) not null,
    errand_id varchar(255),
    expires_at datetime(6),
    created datetime(6),
    created_by_type varchar(16),
    created_by_value varchar(255),
    primary key (id)
) engine=InnoDB;

create index if not exists idx_subscription_subscriber_id
    on subscription (subscriber_id);

create index if not exists idx_subscription_errand_id
    on subscription (errand_id);

alter table if exists subscription
    add constraint fk_subscription_subscriber_id
    foreign key (subscriber_id) references subscriber (id)
    on delete cascade;

-- Match subscription.errand_id's charset/collation to errand.id before declaring
-- the FK. errand was created in V1_0 under the then-current database default; in
-- environments where the default has since changed (e.g. swedish_ci -> unicode_ci),
-- new tables otherwise pick up the new default and the FK fails with errno 150.
set @errand_id_charset = (select character_set_name from information_schema.columns
    where table_schema = database() and table_name = 'errand' and column_name = 'id');
set @errand_id_collation = (select collation_name from information_schema.columns
    where table_schema = database() and table_name = 'errand' and column_name = 'id');
set @sql := concat('alter table subscription modify errand_id varchar(255) character set ',
    @errand_id_charset, ' collate ', @errand_id_collation);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

alter table if exists subscription
    add constraint fk_subscription_errand_id
    foreign key (errand_id) references errand (id)
    on delete cascade;

create table if not exists subscription_event_filter (
    subscription_id varchar(255) not null,
    type varchar(64) not null,
    subtype varchar(64),
    sort_order integer not null,
    primary key (subscription_id, sort_order)
) engine=InnoDB;

alter table if exists subscription_event_filter
    add constraint fk_subscription_event_filter_subscription_id
    foreign key (subscription_id) references subscription (id)
    on delete cascade;
