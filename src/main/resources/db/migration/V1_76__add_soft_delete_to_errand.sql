-- ============================================================================
-- When an errand was soft deleted and by whom. A soft deleted errand is kept
-- out of every read of the API until it is restored or deleted for good.
-- ============================================================================

alter table errand
    add column if not exists deleted datetime(6) null;

alter table errand
    add column if not exists deleted_by varchar(255) null;
