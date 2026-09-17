package se.sundsvall.supportmanagement.integration.db.model;

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

	@ManyToOne
	@JoinColumn(name = "metadata_label_id",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_errand_labels_metadata_label_id"))
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

	/**
	 * Gives a label that has only just been put together the metadata it points at, which Hibernate fills in only when
	 * the errand is read. Never written: the label is stored by its id alone.
	 *
	 * @param metadataLabel the metadata label with the id this label carries
	 */
	public void setMetadataLabel(MetadataLabelEntity metadataLabel) {
		this.metadataLabel = metadataLabel;
	}

	public ErrandLabelEmbeddable withMetadataLabel(MetadataLabelEntity metadataLabel) {
		this.metadataLabel = metadataLabel;
		return this;
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
