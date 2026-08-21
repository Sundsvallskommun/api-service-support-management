create table subscriber_notification_event (
    created    datetime(3)  not null,
    id         varchar(36)  not null,
    notification_id varchar(36) not null,
    event_type varchar(64),
    sub_type   varchar(64),
    description varchar(255),
    primary key (id)
) engine = InnoDB;

insert into subscriber_notification_event (id, created, notification_id, event_type, description, sub_type)
select UUID(), NOW(3), id, event_type, description, sub_type
from subscriber_notification
where event_type is not null;

alter table subscriber_notification_event
    add constraint FK9o94wu44495ht25u34j60shie
        foreign key (notification_id) references subscriber_notification (id);

alter table subscriber_notification
    drop column if exists event_type,
    drop column if exists description,
    drop column if exists sub_type;
