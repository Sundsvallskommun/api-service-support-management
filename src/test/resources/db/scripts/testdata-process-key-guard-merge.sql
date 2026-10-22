-- -----------------------------------------------------------------------------------------------
-- The label naming the application process as the access label of both the errand running that
-- process and the errand that has never had one, on top of testdata-it.sql and
-- testdata-process-key-guard.sql, so that merging it into the label naming the supervision process
-- is asked of both. The errand whose process has finished wears the label without an access label,
-- and is left as it is.
--
-- Kept apart from testdata-process-key-guard.sql: the access labels would show on those errands in
-- every other test that loads that script.
-- -----------------------------------------------------------------------------------------------

INSERT INTO errand_access_labels(errand_id, metadata_label_id)
VALUES ('aa000000-0000-0000-0000-0000000000a1', 'bb000000-0000-0000-0000-0000000000b1'),
       ('aa000000-0000-0000-0000-0000000000a2', 'bb000000-0000-0000-0000-0000000000b1');
