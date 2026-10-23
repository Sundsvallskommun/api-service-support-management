-- ============================================================================
-- A message is logged as MESSAGE_EXTERNAL or MESSAGE_INTERNAL, and MESSAGE is
-- no longer a subtype. A process trigger or an event filter on MESSAGE is
-- therefore split into one on each, which selects what it selected before.
-- ============================================================================

insert into namespace_config_value (namespace_config_id, `key`, `value`, `type`)
    select namespace_config_id, `key`, 'MESSAGE_INTERNAL', `type`
    from namespace_config_value
    where `key` = 'PROCESS_TRIGGER' and `value` = 'MESSAGE';

update namespace_config_value
    set `value` = 'MESSAGE_EXTERNAL'
    where `key` = 'PROCESS_TRIGGER' and `value` = 'MESSAGE';

-- The filters are ordered lists, so the internal half is added after the last
-- filter of its list.
insert into subscriber_event_filter (subscriber_id, sort_order, type, subtype, label_id)
    select f.subscriber_id,
           m.max_order + row_number() over (partition by f.subscriber_id order by f.sort_order),
           f.type, 'MESSAGE_INTERNAL', f.label_id
    from subscriber_event_filter f
    join (select subscriber_id, max(sort_order) as max_order from subscriber_event_filter group by subscriber_id) m
        on m.subscriber_id = f.subscriber_id
    where f.subtype = 'MESSAGE';

insert into subscription_event_filter (subscription_id, sort_order, type, subtype, label_id)
    select f.subscription_id,
           m.max_order + row_number() over (partition by f.subscription_id order by f.sort_order),
           f.type, 'MESSAGE_INTERNAL', f.label_id
    from subscription_event_filter f
    join (select subscription_id, max(sort_order) as max_order from subscription_event_filter group by subscription_id) m
        on m.subscription_id = f.subscription_id
    where f.subtype = 'MESSAGE';

insert into subscription_profile_event_filter (profile_id, sort_order, type, subtype, label_id)
    select f.profile_id,
           m.max_order + row_number() over (partition by f.profile_id order by f.sort_order),
           f.type, 'MESSAGE_INTERNAL', f.label_id
    from subscription_profile_event_filter f
    join (select profile_id, max(sort_order) as max_order from subscription_profile_event_filter group by profile_id) m
        on m.profile_id = f.profile_id
    where f.subtype = 'MESSAGE';

update subscriber_event_filter
    set subtype = 'MESSAGE_EXTERNAL'
    where subtype = 'MESSAGE';

update subscription_event_filter
    set subtype = 'MESSAGE_EXTERNAL'
    where subtype = 'MESSAGE';

update subscription_profile_event_filter
    set subtype = 'MESSAGE_EXTERNAL'
    where subtype = 'MESSAGE';
