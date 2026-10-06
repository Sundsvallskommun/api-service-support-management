-- ============================================================================
-- Enforce "one active label move per namespace" at the DB level to close the
-- TOCTOU race between MetadataService.startLabelMove's hasActiveJob precheck
-- and JobService.create's insert - two requests arriving within milliseconds
-- can both pass the precheck before either commits.
--
-- MariaDB has no native partial/filtered unique index, so the same workaround
-- used in V1_36 applies here: a virtual generated column that is NULL for
-- every row except an active (PENDING or RUNNING) MOVE_LABEL job, on which it
-- carries the namespace and municipality the job belongs to. MariaDB treats
-- NULL as not-equal in UNIQUE indexes, so only rows that both name a move
-- still under way can ever collide - every other row (a different type, a
-- finished move, or both) is unconstrained.
-- ============================================================================

alter table job
    add column active_move_label_guard varchar(64)
        as (case when type = 'MOVE_LABEL' and status in ('PENDING', 'RUNNING')
                 then concat(namespace, ':', municipality_id)
                 else null
            end) virtual;

alter table job
    add constraint uq_job_active_move_label_per_namespace
    unique (active_move_label_guard);
