-- ============================================================================
-- Whether what a scheduled action does notifies the handler and the
-- subscribers of the errand. Configs that predate the choice notify.
-- ============================================================================

alter table action_config
    add column if not exists notify bit not null default 1;
