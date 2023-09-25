-- Create new table
CREATE TABLE attachment_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    file LONGBLOB,
    attachment_id VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

-- Migrate data
INSERT INTO attachment_data (file, attachment_id)
SELECT file, id FROM attachment;

-- Drop data from attachment
ALTER TABLE attachment DROP COLUMN file;

-- Add foreign key for attachment data
ALTER TABLE attachment ADD COLUMN attachment_data_id INT NOT NULL;

-- Move reference from attachment to attachment_data
UPDATE attachment SET attachment_data_id = (SELECT attachment_data.id FROM attachment_data WHERE attachment_data.attachment_id = attachment.id);
ALTER TABLE attachment_data DROP COLUMN attachment_id;

-- Add constraint
ALTER TABLE attachment
       ADD CONSTRAINT fk_attachment_data_attachment
       FOREIGN KEY (attachment_data_id) REFERENCES attachment_data (id);


