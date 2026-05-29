-- ============================================================================
-- Enforce subscription uniqueness at the DB level to close the TOCTOU race
-- between SubscriptionService.rejectDuplicate (existsBy precheck) and save.
--
-- MariaDB treats NULL as not-equal in UNIQUE indexes, so a straight unique on
-- (subscriber_id, target_type, errand_id) would only protect ERRAND-typed rows
-- (where errand_id is set) and still allow duplicate NAMESPACE rows (errand_id IS NULL).
--
-- Workaround: a virtual generated column that substitutes a sentinel string for
-- NULL errand_id. Real errand_ids are UUIDs so the sentinel can never collide.
-- ============================================================================

alter table subscription
    add column errand_or_namespace_key varchar(255)
        as (coalesce(errand_id, '__namespace__')) virtual;

alter table subscription
    add constraint uq_subscription_subscriber_target_errand
    unique (subscriber_id, target_type, errand_or_namespace_key);
