START TRANSACTION;

UPDATE errand_labels AS el
JOIN errand AS e ON e.id = el.errand_id
JOIN metadata_label AS ml ON ml.name = el.label AND ml.namespace = e.namespace 
SET el.metadata_label_id = ml.id;
    
COMMIT;