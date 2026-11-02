package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "external_id_type",
	indexes = {
		@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_namespace_municipality_id_name", columnNames = {
			"namespace", "municipality_id", "name"
		})
	})
public class ExternalIdTypeEntity extends AbstractNamedMetadataEntity<ExternalIdTypeEntity> {

	public static ExternalIdTypeEntity create() {
		return new ExternalIdTypeEntity();
	}
}
