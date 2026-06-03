package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A selectable metadata option identified by its technical name and display name")
public class MetadataOption {

	@Schema(description = "Technical name of the option", examples = "IN_PROGRESS")
	private String name;

	@Schema(description = "Human readable display name of the option", examples = "Pågående")
	private String displayName;

	public static MetadataOption create() {
		return new MetadataOption();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public MetadataOption withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public MetadataOption withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, displayName);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final MetadataOption other)) {
			return false;
		}
		return Objects.equals(name, other.name) && Objects.equals(displayName, other.displayName);
	}

	@Override
	public String toString() {
		return "MetadataOption [name=" + name + ", displayName=" + displayName + "]";
	}
}
