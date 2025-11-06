package se.sundsvall.supportmanagement.integration.db.model;

import static jakarta.persistence.ConstraintMode.NO_CONSTRAINT;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;

@Embeddable
public class ErrandLabelEmbeddable {

	@Column(name = "metadata_label_id", nullable = false)
	private String metadataLabelId;

	// TODO: Remove the NO_CONSTRAINT when migration is performed
	@ManyToOne
	@JoinColumn(name = "metadata_label_id",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_metadata_label_id", value = NO_CONSTRAINT))
	private MetadataLabelEntity metadataLabel;

	public static ErrandLabelEmbeddable create() {
		return new ErrandLabelEmbeddable();
	}

	public String getMetadataLabelId() {
		return metadataLabelId;
	}

	public void setMetadataLabelId(String metadataLabelId) {
		this.metadataLabelId = metadataLabelId;
	}

	public ErrandLabelEmbeddable withMetadataLabelId(String metadataLabelId) {
		this.metadataLabelId = metadataLabelId;
		return this;
	}

	public MetadataLabelEntity getMetadataLabel() {
		return metadataLabel;
	}

	@Override
	public int hashCode() {
		return Objects.hash(metadataLabelId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ErrandLabelEmbeddable other = (ErrandLabelEmbeddable) obj;
		return Objects.equals(metadataLabelId, other.metadataLabelId);
	}

	@Override
	public String toString() {
		return "ErrandLabelEmbeddable [metadataLabelId=" + metadataLabelId + "]";
	}
}
