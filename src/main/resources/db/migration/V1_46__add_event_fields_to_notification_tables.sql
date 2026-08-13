alter table notification_dispatch
    add column if not exists description varchar(255),
    add column if not exists sub_type    varchar(64);

alter table subscriber_notification
    add column if not exists event_type  varchar(64),
    add column if not exists description varchar(255),
    add column if not exists sub_type    varchar(64);
