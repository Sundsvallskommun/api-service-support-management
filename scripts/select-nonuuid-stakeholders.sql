-- Lists stakeholder rows whose external_id is NOT a valid UUID (e.g. an organisationsnummer
-- or personnummer that should be converted to a partyId).
--
-- Run this in DBeaver and export the result grid to CSV (comma-separated, with header).
-- Feed that CSV into scripts/generate-partyid-update-sql.sh to produce the UPDATE statements.
--
-- Expected/required column order for the export: id, external_id, external_id_type, municipality_id
SELECT s.id                 AS id,
       s.external_id         AS external_id,
       s.external_id_type    AS external_id_type,
       e.municipality_id     AS municipality_id
FROM stakeholder s
JOIN errand e ON e.id = s.errand_id
WHERE s.external_id IS NOT NULL
  AND s.external_id <> ''
  AND s.external_id NOT REGEXP '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
ORDER BY s.id;
