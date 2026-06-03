package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Mapping suggestion for the namespace-bound classification (category/type) field")
public class ClassificationMapping {

	@Schema(description = "Classification on the source errand")
	private ClassificationOption source;

	@Schema(description = "Auto-suggested target category name, or null if no match was found", examples = "SUPPORT_CASE")
	private String suggestedCategory;

	@Schema(description = "Auto-suggested target type name, or null if no match was found", examples = "OTHER_ISSUES")
	private String suggestedType;

	@Schema(description = "Selectable types per category in the target namespace. Always present, may be empty", requiredMode = REQUIRED)
	private Map<String, List<String>> candidates;

	public static ClassificationMapping create() {
		return new ClassificationMapping();
	}

	public ClassificationOption getSource() {
		return source;
	}

	public void setSource(final ClassificationOption source) {
		this.source = source;
	}

	public ClassificationMapping withSource(final ClassificationOption source) {
		this.source = source;
		return this;
	}

	public String getSuggestedCategory() {
		return suggestedCategory;
	}

	public void setSuggestedCategory(final String suggestedCategory) {
		this.suggestedCategory = suggestedCategory;
	}

	public ClassificationMapping withSuggestedCategory(final String suggestedCategory) {
		this.suggestedCategory = suggestedCategory;
		return this;
	}

	public String getSuggestedType() {
		return suggestedType;
	}

	public void setSuggestedType(final String suggestedType) {
		this.suggestedType = suggestedType;
	}

	public ClassificationMapping withSuggestedType(final String suggestedType) {
		this.suggestedType = suggestedType;
		return this;
	}

	public Map<String, List<String>> getCandidates() {
		return candidates;
	}

	public void setCandidates(final Map<String, List<String>> candidates) {
		this.candidates = candidates;
	}

	public ClassificationMapping withCandidates(final Map<String, List<String>> candidates) {
		this.candidates = candidates;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, suggestedCategory, suggestedType, candidates);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ClassificationMapping other)) {
			return false;
		}
		return Objects.equals(source, other.source) && Objects.equals(suggestedCategory, other.suggestedCategory)
			&& Objects.equals(suggestedType, other.suggestedType) && Objects.equals(candidates, other.candidates);
	}

	@Override
	public String toString() {
		return "ClassificationMapping [source=" + source + ", suggestedCategory=" + suggestedCategory + ", suggestedType=" + suggestedType + ", candidates=" + candidates + "]";
	}
}
