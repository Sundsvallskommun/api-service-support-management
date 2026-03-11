-- Add configurable auto-reply and no-reply toggles to email worker config
ALTER TABLE email_worker_config ADD COLUMN ignore_auto_reply bit DEFAULT 0;
ALTER TABLE email_worker_config ADD COLUMN ignore_no_reply bit DEFAULT 0;

-- Add new email header enum values for Return-Path and Content-Type
ALTER TABLE communication_email_header
    MODIFY header_key ENUM('AUTO_SUBMITTED', 'CONTENT_TYPE', 'IN_REPLY_TO', 'MESSAGE_ID', 'REFERENCES', 'RETURN_PATH');
