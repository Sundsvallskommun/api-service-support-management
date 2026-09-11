package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * What can be written about the link between a handling artefact and an attachment of the errand.
 * <p>
 * Deliberately not {@link ArtefactAttachment}, which describes the attachment and is what the link operations answer
 * with. Everything else that describes an attachment - its name, its type, its size, what it is for - belongs to the
 * attachment and is written through the attachment resource of the errand. The order it is shown in is the one thing
 * that belongs to the link, since the same attachment can sit in different places under different artefacts.
 */
@Schema(description = "Writable properties of a link between a handling artefact and an attachment")
public class ArtefactAttachmentLink {

	@Schema(description = "Order the attachment is shown in under the artefact", examples = "1")
	private Integer sortOrder;

	public static ArtefactAttachmentLink create() {
		return new ArtefactAttachmentLink();
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public ArtefactAttachmentLink withSortOrder(final Integer sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sortOrder);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ArtefactAttachmentLink other)) {
			return false;
		}
		return Objects.equals(sortOrder, other.sortOrder);
	}

	@Override
	public String toString() {
		return "ArtefactAttachmentLink{" +
			"sortOrder=" + sortOrder +
			'}';
	}
}
