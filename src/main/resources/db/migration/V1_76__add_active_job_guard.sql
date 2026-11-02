-- ============================================================================
-- Enforce "one active job of a kind per namespace" at the DB level to close the
-- TOCTOU race between a caller's own precheck (JobService.hasActiveJob /
-- stealStaleLease) and JobService.launch's insert (via createJob) - two
-- requests arriving within milliseconds of each other can both pass the
-- precheck before either commits.
--
-- Generalized across every job type rather than hardcoded to one, since the
-- guard MOVE_LABEL needed is exactly the guard any future kind of job needs
-- too - a second kind (ERRAND_PURGE) already wants the same one-per-namespace
-- exclusivity, today only enforced racily at the application level.
--
-- MariaDB has no native partial/filtered unique index, so the same workaround
-- used in V1_36 applies here: a virtual generated column that is NULL for
-- every row except an active (PENDING or RUNNING) job, on which it carries the
-- type, namespace and municipality the job belongs to. MariaDB treats NULL as
-- not-equal in UNIQUE indexes, so only two rows that both name a still-active
-- job of the same kind in the same namespace can ever collide - every other
-- row (a different type, a different namespace, or a finished run) is
-- unconstrained.
-- ============================================================================

alter table job
    add column active_job_guard varchar(96)
        as (case when status in ('PENDING', 'RUNNING')
                 then concat(type, ':', namespace, ':', municipality_id)
                 else null
            end) virtual;

alter table job
    add constraint uq_job_active_per_type_per_namespace
    unique (active_job_guard);
