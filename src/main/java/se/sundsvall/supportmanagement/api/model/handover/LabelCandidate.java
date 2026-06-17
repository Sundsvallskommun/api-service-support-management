package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A label that can be chosen as the target for a source label")
public class LabelCandidate {

	@Schema(description = "Unique id of the label in the target namespace", examples = "b82bd8ac-1507-4d9a-958d-369261eecc15")
	private String id;

	@Schema(description = "Display name of the label", examples = "Nyckelkort")
	private String displayName;

	@Schema(description = "Hierarchical resource path of the label", examples = "/access/keycard")
	private String resourcePath;

	public static LabelCandidate create() {
		return new LabelCandidate();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public LabelCandidate withId(final String id) {
		this.id = id;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public LabelCandidate withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getResourcePath() {
		return resourcePath;
	}

	public void setResourcePath(final String resourcePath) {
		this.resourcePath = resourcePath;
	}

	public LabelCandidate withResourcePath(final String resourcePath) {
		this.resourcePath = resourcePath;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, displayName, resourcePath);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final LabelCandidate other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(displayName, other.displayName) && Objects.equals(resourcePath, other.resourcePath);
	}

	@Override
	public String toString() {
		return "LabelCandidate [id=" + id + ", displayName=" + displayName + ", resourcePath=" + resourcePath + "]";
	}
}
