-- -----------------------------------------------------------------------------------------------
-- The errand running a process in testdata-it.sql, assigned to someone other than the handler
-- sending the signals, so that a notification would have someone to go to.
-- -----------------------------------------------------------------------------------------------
UPDATE errand
SET assigned_user_id = 'ann01doe'
WHERE id = 'aa000000-0000-0000-0000-0000000000a1';
