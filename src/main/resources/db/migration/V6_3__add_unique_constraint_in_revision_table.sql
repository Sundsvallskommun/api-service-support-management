alter table if exists revision 
    add constraint uq_entity_id_version unique (version, entity_id);