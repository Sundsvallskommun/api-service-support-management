-- Composite index supporting duplicate-detection queries in SubscriptionRepository:
--   existsBySubscriberIdAndTargetTypeAndErrandId(...)
--   existsBySubscriberIdAndTargetTypeAndErrandIsNull(...)
create index if not exists idx_subscription_subscriber_target
    on subscription (subscriber_id, target_type, errand_id);

-- Drop redundant single-column index: the new composite has subscriber_id as its leading
-- column, so it already serves queries that only filter by subscriber_id.
drop index if exists idx_subscription_subscriber_id on subscription;
