-- ============================================================================
-- A principal leaving a subscription profile is remembered here, so the
-- members sync does not subscribe them to it again. Kept against the
-- principal rather than a subscriber, so it holds whichever subscriber the
-- sync would use and outlives the principal deleting their subscribers.
-- Removing the profile removes the record.
-- ============================================================================

create table if not exists subscription_opt_out (
    id varchar(255) not null,
    profile_id varchar(255) not null,
    identifier_type varchar(16) not null,
    identifier_value varchar(255) not null,
    created datetime(6),
    primary key (id)
) engine=InnoDB;

alter table if exists subscription_opt_out
    add constraint uq_subscription_opt_out_profile_identifier
    unique (profile_id, identifier_type, identifier_value);

alter table if exists subscription_opt_out
    add constraint fk_subscription_opt_out_profile_id
    foreign key (profile_id) references subscription_profile (id)
    on delete cascade;
