START TRANSACTION;

UPDATE
    errand_labels AS el
JOIN metadata_label AS ml ON
    el.label = ml.name
SET
    el.metadata_label_id = ml.id;
    
COMMIT;