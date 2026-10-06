-- ============================================================================
-- Enforce "one active label-tree restructure per namespace" at the DB level,
-- mirroring V1_60__add_active_label_move_guard.sql / V1_61__add_active_merge_
-- labels_guard.sql for RESTRUCTURE_LABEL_TREE jobs - the same TOCTOU race
-- exists between MetadataService.startLabelTreeRestructure's hasActiveJob
-- precheck and JobService.create's insert.
--
-- A separate guard column, scoped only to RESTRUCTURE_LABEL_TREE - it does not
-- by itself stop a restructure from racing a concurrent MOVE_LABEL/MERGE_LABELS
-- job (that cross-type case is covered by startLabelTreeRestructure's app-level
-- precheck using the type-agnostic JobService.hasActiveJob(namespace,
-- municipalityId) overload, not a DB constraint - closing it at the DB level
-- too would need a guard whose value depends on every row in the namespace
-- rather than the row's own type/status, which does not fit this column-per-row
-- pattern and is not needed yet).
-- ============================================================================

alter table job
    add column active_restructure_label_tree_guard varchar(64)
        as (case when type = 'RESTRUCTURE_LABEL_TREE' and status in ('PENDING', 'RUNNING')
                 then concat(namespace, ':', municipality_id)
                 else null
            end) virtual;

alter table job
    add constraint uq_job_active_restructure_label_tree_per_namespace
    unique (active_restructure_label_tree_guard);
