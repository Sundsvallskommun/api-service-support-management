package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "role",
	indexes = {
		@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_namespace_municipality_id_name", columnNames = {
			"namespace", "municipality_id", "name"
		})
	})
public class RoleEntity extends AbstractNamedMetadataEntity<RoleEntity> {

	public static RoleEntity create() {
		return new RoleEntity();
	}
}
