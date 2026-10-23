-- ============================================================================
-- The email dispatch outbox becomes the outbox of every channel delivered
-- after the dispatch, which now also holds the sms waiting to be sent. What is
-- already in it was enqueued for email.
-- ============================================================================

alter table email_dispatch_outbox_event
    drop foreign key if exists fk_email_dispatch_outbox_event_outbox_id;

-- The index MariaDB created along with the foreign key, which is recreated under the new name below
alter table email_dispatch_outbox_event
    drop index if exists fk_email_dispatch_outbox_event_outbox_id;

alter table email_dispatch_outbox
    drop foreign key if exists fk_email_dispatch_outbox_subscriber_id;

rename table email_dispatch_outbox to notification_dispatch_outbox,
    email_dispatch_outbox_event to notification_dispatch_outbox_event;

alter table notification_dispatch_outbox
    add column if not exists channel varchar(32) not null default 'EMAIL' after subscriber_id;

alter table notification_dispatch_outbox
    alter column channel drop default;

alter table notification_dispatch_outbox
    drop index if exists idx_email_dispatch_outbox_subscriber_created;

create index if not exists idx_notification_dispatch_outbox_subscriber_channel_created
    on notification_dispatch_outbox (subscriber_id, channel, created);

alter table notification_dispatch_outbox
    add constraint fk_notification_dispatch_outbox_subscriber_id
    foreign key (subscriber_id) references subscriber (id)
    on delete cascade;

alter table notification_dispatch_outbox_event
    add constraint fk_notification_dispatch_outbox_event_outbox_id
    foreign key (outbox_id) references notification_dispatch_outbox (id)
    on delete cascade;
