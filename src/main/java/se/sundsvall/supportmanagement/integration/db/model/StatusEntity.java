package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(name = "status",
	indexes = {
		@Index(name = "idx_namespace_municipality_id", columnList = "namespace, municipality_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_namespace_municipality_id_name", columnNames = {
			"namespace", "municipality_id", "name"
		})
	})
public class StatusEntity extends AbstractNamedMetadataEntity<StatusEntity> {

	@Column(name = "external_display_name")
	private String externalDisplayName;

	public static StatusEntity create() {
		return new StatusEntity();
	}

	public String getExternalDisplayName() {
		return externalDisplayName;
	}

	public void setExternalDisplayName(final String externalDisplayName) {
		this.externalDisplayName = externalDisplayName;
	}

	public StatusEntity withExternalDisplayName(final String externalDisplayName) {
		this.externalDisplayName = externalDisplayName;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		return super.equals(o) && Objects.equals(externalDisplayName, ((StatusEntity) o).externalDisplayName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), externalDisplayName);
	}

	@Override
	protected String fieldsToString() {
		return super.fieldsToString() + ", externalDisplayName='" + externalDisplayName + '\'';
	}
}
