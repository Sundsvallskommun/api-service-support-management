package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class AccessLabelEmbeddable {

	@Column(name = "metadata_label_id", nullable = false)
	private String metadataLabelId;

	public static AccessLabelEmbeddable create() {
		return new AccessLabelEmbeddable();
	}

	public String getMetadataLabelId() {
		return metadataLabelId;
	}

	public void setMetadataLabelId(String metadataLabelId) {
		this.metadataLabelId = metadataLabelId;
	}

	public AccessLabelEmbeddable withMetadataLabelId(String metadataLabelId) {
		this.metadataLabelId = metadataLabelId;
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
		AccessLabelEmbeddable other = (AccessLabelEmbeddable) obj;
		return Objects.equals(metadataLabelId, other.metadataLabelId);
	}

	@Override
	public String toString() {
		return "AccessLabelEmbeddable [metadataLabelId=" + metadataLabelId + "]";
	}
}
