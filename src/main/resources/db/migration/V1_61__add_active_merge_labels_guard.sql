-- ============================================================================
-- Enforce "one active label merge per namespace" at the DB level, mirroring
-- V1_60__add_active_label_move_guard.sql for MERGE_LABELS jobs instead of
-- MOVE_LABEL - the same TOCTOU race exists between
-- MetadataService.startLabelMerge's hasActiveJob precheck and
-- JobService.create's insert.
--
-- A separate guard column from V1_60's, not a shared one: each covers only
-- its own job type, so a move and a merge may still run concurrently in the
-- same namespace, exactly as two different move-only or merge-only guards
-- would allow no more and no less. Narrowing that further is not needed yet.
-- ============================================================================

alter table job
    add column active_merge_labels_guard varchar(64)
        as (case when type = 'MERGE_LABELS' and status in ('PENDING', 'RUNNING')
                 then concat(namespace, ':', municipality_id)
                 else null
            end) virtual;

alter table job
    add constraint uq_job_active_merge_labels_per_namespace
    unique (active_merge_labels_guard);
