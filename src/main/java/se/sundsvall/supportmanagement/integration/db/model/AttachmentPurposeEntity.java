package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * The purposes an attachment of the errand may be given, registered per namespace.
 * <p>
 * The recommended values - SUPPORTING, RESPONSE, DECISION, APPENDIX, PROTOCOL - are not seeded. A namespace registers
 * the ones it uses, and a purpose it has not registered is rejected.
 */
@Entity
@Table(name = "attachment_purpose",
	indexes = {
		@Index(name = "idx_attachment_purpose_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_attachment_purpose_namespace_municipality_id_name", columnNames = {
			"namespace", "municipality_id", "name"
		})
	})
public class AttachmentPurposeEntity extends AbstractNamedMetadataEntity<AttachmentPurposeEntity> {

	public static AttachmentPurposeEntity create() {
		return new AttachmentPurposeEntity();
	}
}
