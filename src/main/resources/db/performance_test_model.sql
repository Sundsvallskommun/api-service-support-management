--------------------------------------------------
-- Beskrivning:
--
-- Skapar tabellen metadata_label
-- Skapar tabellen metadata_label_authorization (id, metadata_label_id, authorization_id)
-- Populera tabellen metadata_label baserat på vad som finns i tabellerna errand och errand_labels idag.
-- Lägg till kolumnen errand_labels.metadata_label_id
-- Uppdatera tabellen errand_labels så att rätt värde läggs på errand_labels.metadata_label_id
--------------------------------------------------

--------------------------------------------------
-- Skapa tabeller
--------------------------------------------------
CREATE TABLE metadata_label (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created DATETIME(6),
    modified DATETIME(6),
    display_name VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    municipality_id VARCHAR(255) NOT NULL,
    namespace VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE metadata_label_authorization (
    id BIGINT NOT NULL AUTO_INCREMENT,
    metadata_label_id BIGINT NOT NULL,
    authorization_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_metadata_label_authorization_metadata_label
        FOREIGN KEY (metadata_label_id)
        REFERENCES metadata_label (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE errand_labels
    ADD COLUMN IF NOT EXISTS metadata_label_id BIGINT NULL;

--------------------------------------------------
-- Populera metadata_label
--------------------------------------------------
INSERT INTO metadata_label (created, modified, display_name, name, municipality_id, namespace)
SELECT 
    NOW(),
    NOW(),
    el.label,
    el.label,
    MIN(e.municipality_id),
    MIN(e.namespace)
FROM errand_labels el
JOIN errand e ON e.id = el.errand_id
GROUP BY el.label;

--------------------------------------------------
-- Populera errand_labels.metadata_label_id
--------------------------------------------------
UPDATE errand_labels AS el
JOIN metadata_label AS ml ON el.label = ml.name
SET el.metadata_label_id = ml.id;

--------------------------------------------------
-- Sätt upp FK-relation så att data förblir konsistent
--------------------------------------------------
ALTER TABLE errand_labels
ADD CONSTRAINT fk_errand_labels_metadata_label
FOREIGN KEY (metadata_label_id)
REFERENCES metadata_label(id)
ON DELETE SET NULL;
