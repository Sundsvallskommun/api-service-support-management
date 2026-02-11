
    create table action_config (
        active bit not null,
        created datetime(6),
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        display_value varchar(255),
        id varchar(255) not null,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table action_config_condition (
        action_config_id varchar(255) not null,
        condition_key varchar(255),
        id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table action_config_condition_values (
        value_order integer default 0 not null,
        value varchar(2000),
        action_config_condition_id varchar(255) not null,
        primary key (value_order, action_config_condition_id)
    ) engine=InnoDB;

    create table action_config_parameter (
        action_config_id varchar(255) not null,
        id varchar(255) not null,
        parameter_key varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table action_config_parameter_values (
        value_order integer default 0 not null,
        value varchar(2000),
        action_config_parameter_id varchar(255) not null,
        primary key (value_order, action_config_parameter_id)
    ) engine=InnoDB;

    create table attachment (
        attachment_data_id integer not null,
        file_size integer,
        created datetime(6),
        modified datetime(6),
        municipality_id varchar(8),
        namespace varchar(32),
        errand_id varchar(255) not null,
        file_name varchar(255),
        id varchar(255) not null,
        mime_type varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table attachment_data (
        id integer not null auto_increment,
        file longblob,
        primary key (id)
    ) engine=InnoDB;

    create table category (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        display_name varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table communication (
        internal bit,
        viewed bit,
        municipality_id varchar(8),
        sent datetime(6),
        namespace varchar(32),
        errand_number varchar(255),
        external_id varchar(255),
        id varchar(255) not null,
        sender varchar(255),
        sender_user_id varchar(255),
        subject varchar(255),
        target varchar(255),
        type varchar(255) not null,
        direction enum ('INBOUND','OUTBOUND'),
        html_message_body longtext,
        message_body longtext,
        primary key (id)
    ) engine=InnoDB;

    create table communication_attachment (
        attachment_data_id integer not null,
        file_size integer,
        municipality_id varchar(8),
        namespace varchar(32),
        communication_id varchar(255) not null,
        file_name varchar(255),
        id varchar(255) not null,
        mime_type varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table communication_email_header (
        communication_id varchar(255),
        id varchar(255) not null,
        header_key enum ('AUTO_SUBMITTED','IN_REPLY_TO','MESSAGE_ID','REFERENCES'),
        primary key (id)
    ) engine=InnoDB;

    create table communication_email_header_value (
        order_index integer not null,
        value varchar(2048),
        header_id varchar(255) not null,
        primary key (order_index, header_id)
    ) engine=InnoDB;

    create table communication_errand_attachment (
        communication_id varchar(255) not null,
        errand_attachment_id varchar(255) not null
    ) engine=InnoDB;

    create table communication_recipients (
        communication_id varchar(255) not null,
        recipient varchar(255)
    ) engine=InnoDB;

    create table contact_channel (
        stakeholder_id bigint not null,
        type varchar(255),
        value varchar(255)
    ) engine=InnoDB;

    create table contact_reason (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        reason varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table conversation (
        latest_synced_sequence_number bigint,
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        type varchar(32) not null,
        errand_id varchar(36) not null,
        id varchar(36) not null,
        message_exchange_id varchar(36) not null,
        target_relation_id varchar(36),
        topic varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table conversation_relation_id (
        conversation_id varchar(36) not null,
        relation_id varchar(36)
    ) engine=InnoDB;

    create table email_worker_config (
        add_sender_as_stakeholder bit,
        days_of_inactivity_before_reject integer,
        enabled bit not null,
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        errand_closed_email_template varchar(5000),
        errand_new_email_template varchar(5000),
        errand_channel varchar(255),
        errand_closed_email_html_template TEXT,
        errand_closed_email_sender varchar(255),
        errand_new_email_html_template TEXT,
        errand_new_email_sender varchar(255),
        inactive_status varchar(255),
        stakeholder_role varchar(255),
        status_change_to varchar(255),
        status_for_new varchar(255),
        trigger_status_change_on varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table errand (
        business_related bit,
        contact_reason_id bigint,
        created datetime(6),
        modified datetime(6),
        municipality_id varchar(8) not null,
        suspended_from datetime(6),
        suspended_to datetime(6),
        touched datetime(6),
        namespace varchar(32) not null,
        status varchar(64),
        type varchar(128),
        contact_reason_description varchar(4096),
        assigned_group_id varchar(255),
        assigned_user_id varchar(255),
        category varchar(255),
        channel varchar(255),
        errand_number varchar(255) not null,
        escalation_email varchar(255),
        id varchar(255) not null,
        previous_status varchar(255),
        priority varchar(255),
        reporter_user_id varchar(255),
        resolution varchar(255),
        title varchar(255),
        description longtext,
        primary key (id)
    ) engine=InnoDB;

    create table errand_labels (
        errand_id varchar(255) not null,
        metadata_label_id varchar(255)
    ) engine=InnoDB;

    create table errand_number_sequence (
        last_sequence_number integer,
        reset_year_month varchar(6),
        municipality_id varchar(8),
        namespace varchar(32) not null,
        primary key (namespace)
    ) engine=InnoDB;

    create table external_id_type (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table external_tag (
        errand_id varchar(255) not null,
        `key` varchar(255),
        `value` varchar(255)
    ) engine=InnoDB;

    create table json_parameter (
        json_parameter_order integer default 0 not null,
        errand_id varchar(255) not null,
        id varchar(255) not null,
        parameter_key varchar(255),
        schema_id varchar(255),
        value longtext,
        primary key (id)
    ) engine=InnoDB;

    create table message_exchange_sync (
        active bit,
        id bigint not null auto_increment,
        latest_synced_sequence_number bigint default 0,
        municipality_id varchar(8) not null,
        updated datetime(6),
        namespace varchar(32) not null,
        primary key (id)
    ) engine=InnoDB;

    create table metadata_label (
        created datetime(6),
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        classification varchar(255),
        display_name varchar(255),
        id varchar(255) not null,
        parent_id varchar(255),
        resource_name varchar(255),
        resource_path varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table namespace_config (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        primary key (id)
    ) engine=InnoDB;

    create table namespace_config_value (
        namespace_config_id bigint not null,
        `key` varchar(255) not null,
        `type` varchar(255) not null,
        `value` text not null
    ) engine=InnoDB;

    create table notification (
        acknowledged bit not null,
        global_acknowledged bit not null,
        created datetime(6),
        expires datetime(6),
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        content varchar(255),
        created_by varchar(255),
        created_by_full_name varchar(255),
        description varchar(255),
        errand_id varchar(255),
        id varchar(255) not null,
        owner_full_name varchar(255),
        owner_id varchar(255),
        subtype varchar(255),
        type varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table parameter (
        parameter_order integer default 0 not null,
        display_name varchar(255),
        errand_id varchar(255) not null,
        id varchar(255) not null,
        parameter_group varchar(255),
        parameters_key varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table parameter_values (
        value_order integer default 0 not null,
        value varchar(2000),
        parameter_id varchar(255) not null,
        primary key (value_order, parameter_id)
    ) engine=InnoDB;

    create table revision (
        version integer,
        created datetime(6),
        municipality_id varchar(8),
        namespace varchar(32),
        entity_id varchar(255),
        entity_type varchar(255),
        id varchar(255) not null,
        serialized_snapshot longtext,
        primary key (id)
    ) engine=InnoDB;

    create table role (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        display_name varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table stakeholder (
        id bigint not null auto_increment,
        address varchar(255),
        care_of varchar(255),
        city varchar(255),
        country varchar(255),
        errand_id varchar(255) not null,
        external_id varchar(255),
        external_id_type varchar(255),
        first_name varchar(255),
        last_name varchar(255),
        organization_name varchar(255),
        role varchar(255),
        zip_code varchar(255),
        primary key (id)
    ) engine=InnoDB;

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

    create table status (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table time_measurement (
        id bigint not null auto_increment,
        start_time datetime(6),
        stop_time datetime(6),
        administrator varchar(255),
        description varchar(255),
        errand_id varchar(255),
        status varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table `type` (
        category_id bigint not null,
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        display_name varchar(255),
        escalation_email varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table validation (
        validated bit,
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        `type` varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table web_message_collect (
        id bigint not null auto_increment,
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        instance varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table web_message_collect_family_ids (
        web_message_collect_id bigint not null,
        family_id varchar(255)
    ) engine=InnoDB;

    create index idx_action_config_municipality_id_namespace 
       on action_config (municipality_id, namespace);

    create index idx_attachment_file_name 
       on attachment (file_name);

    create index idx_attachment_municipality_id 
       on attachment (municipality_id);

    create index idx_attachment_namespace 
       on attachment (namespace);

    alter table if exists attachment 
       add constraint uq_attachment_data_id unique (attachment_data_id);

    create index idx_namespace_municipality_id 
       on category (namespace, municipality_id);

    alter table if exists category 
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);

    create index idx_errand_number 
       on communication (errand_number);

    create index idx_communication_namespace 
       on communication (namespace);

    create index idx_communication_municipality_id 
       on communication (municipality_id);

    create index idx_communication_attachment_municipality_id 
       on communication_attachment (municipality_id);

    create index idx_communication_attachment_namespace 
       on communication_attachment (namespace);

    alter table if exists communication_attachment 
       add constraint uq_attachment_data_id unique (attachment_data_id);

    create index idx_contact_channel_type_value 
       on contact_channel (type, value);

    create index idx_contact_channel_value 
       on contact_channel (value);

    create index idx_municipality_id_namespace_errand_id 
       on conversation (municipality_id, namespace, errand_id);

    create index idx_message_exchange_id 
       on conversation (message_exchange_id);

    create index idx_namespace_municipality_id 
       on email_worker_config (namespace, municipality_id);

    alter table if exists email_worker_config 
       add constraint uq_namespace_municipality_id unique (namespace, municipality_id);

    create index idx_errand_id 
       on errand (id);

    create index idx_errand_namespace 
       on errand (namespace);

    create index idx_errand_municipality_id 
       on errand (municipality_id);

    create index idx_errand_municipality_id_namespace_status 
       on errand (municipality_id, namespace, status);

    create index idx_errand_municipality_id_namespace_category 
       on errand (municipality_id, namespace, category);

    create index idx_errand_municipality_id_namespace_type 
       on errand (municipality_id, namespace, type);

    create index idx_errand_municipality_id_namespace_assigned_user_id 
       on errand (municipality_id, namespace, assigned_user_id);

    create index idx_errand_municipality_id_namespace_reporter_user_id 
       on errand (municipality_id, namespace, reporter_user_id);

    create index idx_errand_errand_number 
       on errand (errand_number);

    create index idx_errand_municipality_id_namespace_status_created 
       on errand (municipality_id, namespace, status, created);

    create index idx_errand_suspended_to 
       on errand (suspended_to);

    create index idx_errand_channel 
       on errand (channel);

    create index idx_errand_municipality_id_namespace_status_touched 
       on errand (municipality_id, namespace, status, touched);

    create index idx_errand_municipality_id_namespace_status_modified 
       on errand (municipality_id, namespace, status, modified);

    create index idx_errand_municipality_id_namespace_created 
       on errand (municipality_id, namespace, created);

    create index idx_errand_municipality_id_namespace_touched 
       on errand (municipality_id, namespace, touched);

    alter table if exists errand 
       add constraint uq_errand_number unique (errand_number);

    create index idx_errand_id_metadata_label_id 
       on errand_labels (errand_id, metadata_label_id);

    create index idx_metadata_label_id_errand_id 
       on errand_labels (metadata_label_id, errand_id);

    create index idx_errand_id 
       on errand_labels (errand_id);

    create index idx_metadata_label_id 
       on errand_labels (metadata_label_id);

    create index idx_errand_number_sequence_namespace_municipality_id 
       on errand_number_sequence (namespace, municipality_id);

    create index idx_namespace_municipality_id 
       on external_id_type (namespace, municipality_id);

    alter table if exists external_id_type 
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);

    create index idx_external_tag_errand_id 
       on external_tag (errand_id);

    create index idx_external_tag_key 
       on external_tag (`key`);

    create index idx_external_tag_value 
       on external_tag (`value`);

    alter table if exists external_tag 
       add constraint uq_external_tag_errand_id_key unique (errand_id, `key`);

    create index idx_json_parameter_errand_id 
       on json_parameter (errand_id);

    create index idx_json_parameter_key 
       on json_parameter (parameter_key);

    create index idx_namespace_municipality_id 
       on metadata_label (namespace, municipality_id);

    create index idx_resource_path 
       on metadata_label (resource_path);

    alter table if exists metadata_label 
       add constraint uq_namespace_municipality_id_resource_path unique (namespace, municipality_id, resource_path);

    create index idx_namespace_municipality_id 
       on namespace_config (namespace, municipality_id);

    create index idx_municipality_id 
       on namespace_config (municipality_id);

    alter table if exists namespace_config 
       add constraint uq_namespace_municipality_id unique (namespace, municipality_id);

    create index idx_namespace_config_value_namespace_config_id_key 
       on namespace_config_value (namespace_config_id, `key`);

    alter table if exists namespace_config_value 
       add constraint uk_namespace_config_id_key_value unique (namespace_config_id, `key`, `value`);

    create index idx_namespace_municipality_id 
       on notification (namespace, municipality_id);

    create index idx_notification_municipality_id_namespace_owner_id 
       on notification (municipality_id, namespace, owner_id);

    create index revision_entity_id_index 
       on revision (entity_id);

    create index revision_entity_type_index 
       on revision (entity_type);

    create index revision_municipality_id_index 
       on revision (municipality_id);

    create index revision_namespace_index 
       on revision (namespace);

    alter table if exists revision 
       add constraint uq_entity_id_version unique (entity_id, version);

    create index idx_namespace_municipality_id 
       on role (namespace, municipality_id);

    alter table if exists role 
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);

    create index idx_stakeholder_external_id_role_errand_id 
       on stakeholder (external_id, `role`, errand_id);

    create index idx_namespace_municipality_id 
       on status (namespace, municipality_id);

    alter table if exists status 
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);

    alter table if exists `type` 
       add constraint uq_category_id_name unique (category_id, name);

    create index idx_namespace_municipality_id_type 
       on validation (namespace, municipality_id, type);

    alter table if exists validation 
       add constraint uq_namespace_municipality_id_type unique (namespace, municipality_id, type);

    alter table if exists web_message_collect 
       add constraint uq_namespace_municipality_id_instance_family_id unique (namespace, municipality_id, instance);

    alter table if exists action_config_condition 
       add constraint fk_action_config_condition_action_config_id 
       foreign key (action_config_id) 
       references action_config (id);

    alter table if exists action_config_condition_values 
       add constraint fk_action_config_condition_values_condition_id 
       foreign key (action_config_condition_id) 
       references action_config_condition (id);

    alter table if exists action_config_parameter 
       add constraint fk_action_config_parameter_action_config_id 
       foreign key (action_config_id) 
       references action_config (id);

    alter table if exists action_config_parameter_values 
       add constraint fk_action_config_parameter_values_parameter_id 
       foreign key (action_config_parameter_id) 
       references action_config_parameter (id);

    alter table if exists attachment 
       add constraint fk_attachment_data_attachment 
       foreign key (attachment_data_id) 
       references attachment_data (id);

    alter table if exists attachment 
       add constraint fk_errand_attachment_errand_id 
       foreign key (errand_id) 
       references errand (id);

    alter table if exists communication_attachment 
       add constraint fk_communication_attachment_attachment_data 
       foreign key (attachment_data_id) 
       references attachment_data (id);

    alter table if exists communication_attachment 
       add constraint fk_communication_attachment_communication_id 
       foreign key (communication_id) 
       references communication (id);

    alter table if exists communication_email_header 
       add constraint fk_email_header_email_id 
       foreign key (communication_id) 
       references communication (id);

    alter table if exists communication_email_header_value 
       add constraint fk_header_value_header_id 
       foreign key (header_id) 
       references communication_email_header (id);

    alter table if exists communication_errand_attachment 
       add constraint FKhedy3oimyh7w729ih0ng5etop 
       foreign key (errand_attachment_id) 
       references attachment (id);

    alter table if exists communication_errand_attachment 
       add constraint FKl9pe6hofx8h94egfys403g7n8 
       foreign key (communication_id) 
       references communication (id);

    alter table if exists communication_recipients 
       add constraint fk_communication_recipients_message_id 
       foreign key (communication_id) 
       references communication (id);

    alter table if exists contact_channel 
       add constraint fk_stakeholder_contact_channel_stakeholder_id 
       foreign key (stakeholder_id) 
       references stakeholder (id);

    alter table if exists conversation_relation_id 
       add constraint fk_conversation_relation_conversation_id 
       foreign key (conversation_id) 
       references conversation (id);

    alter table if exists errand 
       add constraint FKeudsxli8chjy568rft33oa79n 
       foreign key (contact_reason_id) 
       references contact_reason (id);

    alter table if exists errand_labels 
       add constraint fk_errand_labels_metadata_label_id 
       foreign key (metadata_label_id) 
       references metadata_label (id);

    alter table if exists errand_labels 
       add constraint fk_errand_labels_errand_id 
       foreign key (errand_id) 
       references errand (id);

    alter table if exists external_tag 
       add constraint fk_errand_external_tag_errand_id 
       foreign key (errand_id) 
       references errand (id);

    alter table if exists json_parameter 
       add constraint fk_json_parameter_errand_id 
       foreign key (errand_id) 
       references errand (id);

    alter table if exists metadata_label 
       add constraint fk_metadata_label_id 
       foreign key (parent_id) 
       references metadata_label (id);

    alter table if exists namespace_config_value 
       add constraint fk_namespace_config_value_namespace_config 
       foreign key (namespace_config_id) 
       references namespace_config (id);

    alter table if exists notification 
       add constraint fk_notification_errand_id 
       foreign key (errand_id) 
       references errand (id);

    alter table if exists parameter 
       add constraint fk_parameter_errand_id 
       foreign key (errand_id) 
       references errand (id);

    alter table if exists parameter_values 
       add constraint fk_parameter_values_parameter_id 
       foreign key (parameter_id) 
       references parameter (id);

    alter table if exists stakeholder 
       add constraint fk_errand_stakeholder_errand_id 
       foreign key (errand_id) 
       references errand (id);

    alter table if exists stakeholder_parameter 
       add constraint fk_stakeholder_parameter_stakeholder_id 
       foreign key (stakeholder_id) 
       references stakeholder (id);

    alter table if exists stakeholder_parameter_values 
       add constraint fk_stakeholder_parameter_values_stakeholder_parameter_id 
       foreign key (stakeholder_parameter_id) 
       references stakeholder_parameter (id);

    alter table if exists time_measurement 
       add constraint fk_errand_time_measure_errand_id 
       foreign key (errand_id) 
       references errand (id);

    alter table if exists `type` 
       add constraint fk_category_id 
       foreign key (category_id) 
       references category (id);

    alter table if exists web_message_collect_family_ids 
       add constraint fk_web_message_collect_family_ids_web_message_collect_id 
       foreign key (web_message_collect_id) 
       references web_message_collect (id);
