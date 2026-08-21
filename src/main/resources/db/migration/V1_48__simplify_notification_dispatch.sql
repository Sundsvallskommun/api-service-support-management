-- Every dispatch now creates its own subscriber notification instead of merging events into an existing one,
-- so a subscriber may hold several notifications for the same errand.
alter table subscriber_notification
    drop index if exists uq_sub_notif_errand_identifier;

-- A failed dispatch is rolled back in full and left in place until it succeeds, which makes retry bookkeeping
-- and dead-lettering obsolete.
drop index if exists idx_dispatch_dead_letter_retry on notification_dispatch;

alter table notification_dispatch
    drop column if exists retry_count,
    drop column if exists next_retry_at,
    drop column if exists dead_letter;
