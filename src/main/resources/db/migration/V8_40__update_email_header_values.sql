alter table if exists communication_email_header
    modify if exists header_key enum ('IN_REPLY_TO', 'REFERENCES', 'MESSAGE_ID', 'AUTO_SUBMITTED') null;
