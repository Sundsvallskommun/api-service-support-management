package se.sundsvall.supportmanagement.api.model.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * The purpose an attachment of the errand is given, as registered for the namespace.
 * <p>
 * The same shape as {@code ErrandLabel}: the id is what is written, and the rest is read from the metadata it points
 * at.
 * A purpose given a new name or display name there is shown as such wherever it is used.
 */
@Schema(description = "Purpose of an errand attachment, as registered for the namespace")
public class ErrandAttachmentPurpose {

	@Schema(description = "AttachmentPurpose ID", examples = "5f79a808-0ef3-4985-99b9-b12f23e202a7")
	@ValidUuid
	private String id;

	@Schema(description = "Name of the attachment purpose", examples = "RESPONSE", accessMode = READ_ONLY)
	private String name;

	@Schema(description = "Display name of the attachment purpose", examples = "Inkommen handling", accessMode = READ_ONLY)
	private String displayName;

	public static ErrandAttachmentPurpose create() {
		return new ErrandAttachmentPurpose();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandAttachmentPurpose withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ErrandAttachmentPurpose withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public ErrandAttachmentPurpose withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, displayName);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ErrandAttachmentPurpose other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(displayName, other.displayName);
	}

	@Override
	public String toString() {
		return "ErrandAttachmentPurpose{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			'}';
	}
}
