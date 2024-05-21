create table time_measure
(
    id            bigint not null auto_increment,
    start_time    datetime(6),
    stop_time     datetime(6),
    administrator varchar(255),
    description   varchar(255),
    status        varchar(255),
    primary key (id)
) engine = InnoDB;
