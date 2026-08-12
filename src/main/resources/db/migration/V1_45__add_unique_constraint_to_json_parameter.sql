alter table json_parameter
    add constraint uq_json_parameter_errand_id_key unique (errand_id, parameter_key);
