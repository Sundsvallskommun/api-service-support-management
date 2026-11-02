-- ============================================================================
-- A message is logged by the channel it went through: an email, an SMS, a web
-- message or a message in a conversation, with or without the outside world.
-- MESSAGE, and MESSAGE_EXTERNAL and MESSAGE_INTERNAL it was once refined into,
-- are no longer subtypes. A process trigger or an event filter on one of them
-- is therefore split into one on each subtype it stands for, which selects
-- what it selected before.
-- ============================================================================

create temporary table message_subtype_split (
    old_subtype varchar(64) not null,
    new_subtype varchar(64) not null,
    position    integer     not null
);

-- The subtype at position 0 takes the place of the old one, the others are
-- added after the last filter of its list.
insert into message_subtype_split (old_subtype, new_subtype, position)
values ('MESSAGE', 'MESSAGE_CONVERSATION_EXTERNAL', 0),
       ('MESSAGE', 'MESSAGE_CONVERSATION_INTERNAL', 1),
       ('MESSAGE', 'MESSAGE_EMAIL_EXTERNAL', 2),
       ('MESSAGE', 'MESSAGE_SMS_EXTERNAL', 3),
       ('MESSAGE', 'MESSAGE_WEB_MESSAGE_EXTERNAL', 4),
       ('MESSAGE_EXTERNAL', 'MESSAGE_CONVERSATION_EXTERNAL', 0),
       ('MESSAGE_EXTERNAL', 'MESSAGE_EMAIL_EXTERNAL', 1),
       ('MESSAGE_EXTERNAL', 'MESSAGE_SMS_EXTERNAL', 2),
       ('MESSAGE_EXTERNAL', 'MESSAGE_WEB_MESSAGE_EXTERNAL', 3),
       ('MESSAGE_INTERNAL', 'MESSAGE_CONVERSATION_INTERNAL', 0);

-- The process triggers are a set, so a subtype the namespace already triggers on
-- is not added again.
insert into namespace_config_value (namespace_config_id, `key`, `value`, `type`)
    select distinct v.namespace_config_id, v.`key`, s.new_subtype, v.`type`
    from namespace_config_value v
    join message_subtype_split s on s.old_subtype = v.`value`
    where v.`key` = 'PROCESS_TRIGGER'
      and not exists (select 1
                      from namespace_config_value existing
                      where existing.namespace_config_id = v.namespace_config_id
                        and existing.`key` = 'PROCESS_TRIGGER'
                        and existing.`value` = s.new_subtype);

delete from namespace_config_value
    where `key` = 'PROCESS_TRIGGER'
      and `value` in ('MESSAGE', 'MESSAGE_EXTERNAL', 'MESSAGE_INTERNAL');

insert into subscriber_event_filter (subscriber_id, sort_order, type, subtype, label_id)
    select f.subscriber_id,
           m.max_order + row_number() over (partition by f.subscriber_id order by f.sort_order, s.position),
           f.type, s.new_subtype, f.label_id
    from subscriber_event_filter f
    join message_subtype_split s on s.old_subtype = f.subtype and s.position > 0
    join (select subscriber_id, max(sort_order) as max_order from subscriber_event_filter group by subscriber_id) m
        on m.subscriber_id = f.subscriber_id;

update subscriber_event_filter f
    join message_subtype_split s on s.old_subtype = f.subtype and s.position = 0
    set f.subtype = s.new_subtype;

insert into subscription_event_filter (subscription_id, sort_order, type, subtype, label_id)
    select f.subscription_id,
           m.max_order + row_number() over (partition by f.subscription_id order by f.sort_order, s.position),
           f.type, s.new_subtype, f.label_id
    from subscription_event_filter f
    join message_subtype_split s on s.old_subtype = f.subtype and s.position > 0
    join (select subscription_id, max(sort_order) as max_order from subscription_event_filter group by subscription_id) m
        on m.subscription_id = f.subscription_id;

update subscription_event_filter f
    join message_subtype_split s on s.old_subtype = f.subtype and s.position = 0
    set f.subtype = s.new_subtype;

insert into subscription_profile_event_filter (profile_id, sort_order, type, subtype, label_id)
    select f.profile_id,
           m.max_order + row_number() over (partition by f.profile_id order by f.sort_order, s.position),
           f.type, s.new_subtype, f.label_id
    from subscription_profile_event_filter f
    join message_subtype_split s on s.old_subtype = f.subtype and s.position > 0
    join (select profile_id, max(sort_order) as max_order from subscription_profile_event_filter group by profile_id) m
        on m.profile_id = f.profile_id;

update subscription_profile_event_filter f
    join message_subtype_split s on s.old_subtype = f.subtype and s.position = 0
    set f.subtype = s.new_subtype;

drop temporary table message_subtype_split;
