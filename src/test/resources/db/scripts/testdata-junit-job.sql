-- Jobs for JobRepositoryTest.
--
-- The moments matter more than the rows. What the queries have to tell apart is a job that has gone quiet from one
-- still being written to, and a job that never got as far as being reported on from one that was, so the timestamps are
-- set relative to now rather than to a date that would drift out of meaning.
INSERT INTO job(id, municipality_id, namespace, type, status, progress, total, processed, message, created, modified)
VALUES ('job-gone-quiet', '2281', 'NAMESPACE-1', 'ERRAND_PURGE', 'RUNNING', 10, 100, 10, null,
        NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY),
       ('job-never-reported-on', '2281', 'NAMESPACE-1', 'ERRAND_PURGE', 'PENDING', 0, 100, 0, null,
        NOW() - INTERVAL 2 DAY, null),
       ('job-being-reported-on', '2281', 'NAMESPACE-1', 'ERRAND_PURGE', 'RUNNING', 50, 100, 50, null,
        NOW() - INTERVAL 3 DAY, NOW()),
       ('job-ended-long-ago', '2281', 'NAMESPACE-1', 'ERRAND_PURGE', 'COMPLETED', 100, 100, 100,
        'Removed 100 of 100 errands reached, 0 could not be removed', NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 39 DAY),
       ('job-ended-just-now', '2281', 'NAMESPACE-1', 'MOVE_LABEL', 'COMPLETED', 100, 100, 100, null,
        NOW() - INTERVAL 1 DAY, NOW());
