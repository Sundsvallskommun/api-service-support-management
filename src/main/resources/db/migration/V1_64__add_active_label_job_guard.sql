-- ============================================================================
-- Enforce "at most one active label-tree job (of any of the three kinds) per
-- namespace" at the DB level, closing a gap the per-type guards in V1_60/V1_61/
-- V1_62 leave open: each of those only blocks a second job of its own exact
-- type, so a MOVE_LABEL and a MERGE_LABELS (or either against a
-- RESTRUCTURE_LABEL_TREE) can both pass MetadataService's own app-level
-- hasActiveJob(namespace, municipalityId) precheck and both insert successfully
-- if they race within the same window - the precheck is type-agnostic, but
-- nothing at the DB level was.
--
-- Same MariaDB generated-virtual-column workaround as the per-type guards: NULL
-- for every row except an active (PENDING or RUNNING) job of one of the three
-- label-tree types, on which it carries the namespace and municipality - so
-- only two such rows in the same namespace can ever collide, regardless of
-- which of the three types either one is.
--
-- Deliberately additive rather than replacing V1_60/V1_61/V1_62: those three
-- guards are now implied by this one (at most one of {move, merge, restructure}
-- active already means at most one of each), so they are redundant but
-- harmless left in place - per this project's migration discipline, a
-- deployed migration is never edited or dropped by a later one just to tidy
-- up what it left behind.
-- ============================================================================

alter table job
    add column active_label_job_guard varchar(64)
        as (case when type in ('MOVE_LABEL', 'MERGE_LABELS', 'RESTRUCTURE_LABEL_TREE') and status in ('PENDING', 'RUNNING')
                 then concat(namespace, ':', municipality_id)
                 else null
            end) virtual;

alter table job
    add constraint uq_job_active_label_job_per_namespace
    unique (active_label_job_guard);
