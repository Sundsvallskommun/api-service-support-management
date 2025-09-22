START TRANSACTION;

CREATE TABLE IF NOT EXISTS stakeholders_update_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stakeholder_id INT,
    old_role VARCHAR(255),
    new_role VARCHAR(255),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO stakeholders_update_log (stakeholder_id, old_role, new_role)
SELECT s.id, s.role, 'SUBJECT'
FROM stakeholder s
JOIN errand e ON s.errand_id = e.id
WHERE s.role = 'PRIMARY'
  AND e.namespace = 'CONTACTSUNDSVALL';

UPDATE stakeholder s
JOIN errand e ON s.errand_id = e.id
SET s.role = 'SUBJECT'
WHERE s.role = 'PRIMARY'
  AND e.namespace = 'CONTACTSUNDSVALL';

COMMIT;