-- ============================================================================
-- A label being added is logged as an event of its own, subtype LABEL, which
-- is the only event carrying the label it added. A filter on a label is
-- therefore moved to that subtype, whatever subtype it was on before, or it
-- would match nothing.
-- ============================================================================

update subscriber_event_filter
    set subtype = 'LABEL'
    where label_id is not null;

update subscription_event_filter
    set subtype = 'LABEL'
    where label_id is not null;

update subscription_profile_event_filter
    set subtype = 'LABEL'
    where label_id is not null;
