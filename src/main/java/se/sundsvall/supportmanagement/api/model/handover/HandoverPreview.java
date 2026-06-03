package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Preview of how a source errand would be handed over to another namespace, without any side effects")
public class HandoverPreview {

	@Schema(description = "Fields that are copied automatically")
	private DirectlyCopyable directlyCopyable;

	@Schema(description = "Namespace-bound fields that require manual mapping")
	private MappingRequired mappingRequired;

	@Schema(description = "Fields that can not be copied to the target namespace. Always present, may be empty", requiredMode = REQUIRED)
	private List<NotCopyable> notCopyable;

	@Schema(description = "Warnings raised while building the preview. Always present, may be empty", requiredMode = REQUIRED)
	private List<Warning> warnings;

	public static HandoverPreview create() {
		return new HandoverPreview();
	}

	public DirectlyCopyable getDirectlyCopyable() {
		return directlyCopyable;
	}

	public void setDirectlyCopyable(final DirectlyCopyable directlyCopyable) {
		this.directlyCopyable = directlyCopyable;
	}

	public HandoverPreview withDirectlyCopyable(final DirectlyCopyable directlyCopyable) {
		this.directlyCopyable = directlyCopyable;
		return this;
	}

	public MappingRequired getMappingRequired() {
		return mappingRequired;
	}

	public void setMappingRequired(final MappingRequired mappingRequired) {
		this.mappingRequired = mappingRequired;
	}

	public HandoverPreview withMappingRequired(final MappingRequired mappingRequired) {
		this.mappingRequired = mappingRequired;
		return this;
	}

	public List<NotCopyable> getNotCopyable() {
		return notCopyable;
	}

	public void setNotCopyable(final List<NotCopyable> notCopyable) {
		this.notCopyable = notCopyable;
	}

	public HandoverPreview withNotCopyable(final List<NotCopyable> notCopyable) {
		this.notCopyable = notCopyable;
		return this;
	}

	public List<Warning> getWarnings() {
		return warnings;
	}

	public void setWarnings(final List<Warning> warnings) {
		this.warnings = warnings;
	}

	public HandoverPreview withWarnings(final List<Warning> warnings) {
		this.warnings = warnings;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(directlyCopyable, mappingRequired, notCopyable, warnings);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final HandoverPreview other)) {
			return false;
		}
		return Objects.equals(directlyCopyable, other.directlyCopyable) && Objects.equals(mappingRequired, other.mappingRequired)
			&& Objects.equals(notCopyable, other.notCopyable) && Objects.equals(warnings, other.warnings);
	}

	@Override
	public String toString() {
		return "HandoverPreview [directlyCopyable=" + directlyCopyable + ", mappingRequired=" + mappingRequired
			+ ", notCopyable=" + notCopyable + ", warnings=" + warnings + "]";
	}
}
