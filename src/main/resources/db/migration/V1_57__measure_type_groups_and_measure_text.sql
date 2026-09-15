-- A measure type carries a set of groups rather than the single one the column held.
create table measure_type_groups (
    measure_type_id varchar(255) not null,
    measure_group varchar(255),
    constraint fk_measure_type_groups_measure_type_id foreign key (measure_type_id) references measure_type (id)
) engine=InnoDB;

insert into measure_type_groups (measure_type_id, measure_group)
select id, measure_group from measure_type where measure_group is not null;

alter table measure_type drop column measure_group;

-- The goal and the description of a measure outgrew what the action plan flow gave them.
alter table measure modify column goal varchar(3000);
alter table measure modify column description varchar(3000);

-- The rework fields of that flow were never filled, and are dropped rather than carried forward.
alter table measure drop column rework_goal;
alter table measure drop column rework_description;
