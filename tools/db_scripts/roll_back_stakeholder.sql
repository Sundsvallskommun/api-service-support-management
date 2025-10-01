START TRANSACTION;

UPDATE stakeholders s
JOIN stakeholders_update_log log ON s.id = log.stakeholder_id
SET s.role = log.old_role
WHERE log.new_role = 'SUBJECT';
  AND s.role = 'SUBJECT';

COMMIT;