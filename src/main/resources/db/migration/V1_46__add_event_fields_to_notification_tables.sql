alter table notification_dispatch
    add column if not exists description varchar(255),
    add column if not exists sub_type    varchar(64);

create table if not exists subscriber_notification_event
(
    id              varchar(36)  not null,
    notification_id varchar(36)  not null,
    created         datetime(3)  not null,
    event_type      varchar(64),
    sub_type        varchar(64),
    description     varchar(255),
    primary key (id),
    constraint fk_sub_notif_event_notification
        foreign key (notification_id) references subscriber_notification (id) on delete cascade
);
