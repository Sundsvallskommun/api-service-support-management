    create table stakeholder_parameter (
        id bigint not null auto_increment,
        stakeholder_id bigint not null,
        display_name varchar(255),
        parameters_key varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table stakeholder_parameter_values (
        stakeholder_parameter_id bigint not null,
        value varchar(255)
    ) engine=InnoDB;

    alter table if exists stakeholder_parameter 
       add constraint fk_stakeholder_parameter_stakeholder_id 
       foreign key (stakeholder_id) 
       references stakeholder (id);

    alter table if exists stakeholder_parameter_values 
       add constraint fk_stakeholder_parameter_values_stakeholder_parameter_id 
       foreign key (stakeholder_parameter_id) 
       references stakeholder_parameter (id);