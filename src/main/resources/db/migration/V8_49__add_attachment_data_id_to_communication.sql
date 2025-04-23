alter table if exists communication_attachment
    add column if not exists attachment_data_id integer null;

alter table if exists communication_attachment
    add constraint fk_communication_attachment_data_attachment
        foreign key if not exists (attachment_data_id)
            references attachment_data (id);

alter table if exists communication_attachment
    modify if exists communication_attachment_data_id bigint null;

start transaction;

-- 1) temp map just holds the old PKs
CREATE TEMPORARY TABLE map_comm_att
(
    communication_attachment_data_id INT PRIMARY KEY,
    attachment_data_id               INT
);

-- 2) seed it with your source IDs in the right order
INSERT INTO map_comm_att (communication_attachment_data_id)
SELECT id
FROM communication_attachment_data
ORDER BY id;

-- 3) bulk‑insert the files (auto‑IDs will go from X → X+N–1)
INSERT INTO attachment_data (file)
SELECT file
FROM communication_attachment_data
ORDER BY id;

-- 4) grab the FIRST new ID into a var
SET @first_new := LAST_INSERT_ID();

-- 5) populate the map table with the full sequence of new IDs
SET @cur := @first_new - 1;
UPDATE map_comm_att
SET attachment_data_id = (@cur := @cur + 1)
ORDER BY communication_attachment_data_id;

-- 6) patch your FK in communication_attachment
UPDATE communication_attachment AS c
    JOIN map_comm_att AS m
    ON c.communication_attachment_data_id = m.communication_attachment_data_id
SET c.attachment_data_id = m.attachment_data_id;

commit;

alter table if exists communication_attachment
    modify if exists attachment_data_id integer not null;
