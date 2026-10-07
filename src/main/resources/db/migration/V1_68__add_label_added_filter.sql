-- ============================================================================
-- Event filters may require that the event added a given metadata label to
-- the errand, which is what notifying of e.g. a raised risk level rests on.
-- ============================================================================

alter table subscriber_event_filter
    add column if not exists label_added varchar(36);

alter table subscription_event_filter
    add column if not exists label_added varchar(36);

alter table subscription_profile_event_filter
    add column if not exists label_added varchar(36);

-- ============================================================================
-- The labels each queued event added to the errand. Every label counts as
-- added for an errand just created.
-- ============================================================================

create table if not exists notification_dispatch_added_label (
    dispatch_id varchar(36) not null,
    metadata_label_id varchar(36) not null,
    primary key (dispatch_id, metadata_label_id)
) engine=InnoDB;

alter table if exists notification_dispatch_added_label
    add constraint fk_notification_dispatch_added_label_dispatch_id
    foreign key (dispatch_id) references notification_dispatch (id)
    on delete cascade;
