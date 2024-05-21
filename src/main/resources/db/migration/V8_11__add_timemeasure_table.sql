create table time_measure
(
    id            bigint       not null auto_increment,
    start_time    datetime(6),
    stop_time     datetime(6),
    administrator varchar(255),
    description   varchar(255),
    errand_id     varchar(255) not null,
    status        varchar(255),
    primary key (id)
) engine = InnoDB;


alter table if exists time_measure
    add constraint fk_errand_time_measure_errand_id
        foreign key (errand_id)
            references errand (id);
