package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Mapping suggestion for the namespace-bound contact reason field")
public class ContactReasonMapping {

	@Schema(description = "Contact reason on the source errand", examples = "Bygglov")
	private String source;

	@Schema(description = "Auto-suggested target contact reason, or null if no match was found", examples = "Bygglov")
	private String suggested;

	@Schema(description = "All selectable contact reasons in the target namespace")
	private List<String> candidates;

	public static ContactReasonMapping create() {
		return new ContactReasonMapping();
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public ContactReasonMapping withSource(final String source) {
		this.source = source;
		return this;
	}

	public String getSuggested() {
		return suggested;
	}

	public void setSuggested(final String suggested) {
		this.suggested = suggested;
	}

	public ContactReasonMapping withSuggested(final String suggested) {
		this.suggested = suggested;
		return this;
	}

	public List<String> getCandidates() {
		return candidates;
	}

	public void setCandidates(final List<String> candidates) {
		this.candidates = candidates;
	}

	public ContactReasonMapping withCandidates(final List<String> candidates) {
		this.candidates = candidates;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, suggested, candidates);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ContactReasonMapping other)) {
			return false;
		}
		return Objects.equals(source, other.source) && Objects.equals(suggested, other.suggested) && Objects.equals(candidates, other.candidates);
	}

	@Override
	public String toString() {
		return "ContactReasonMapping [source=" + source + ", suggested=" + suggested + ", candidates=" + candidates + "]";
	}
}
