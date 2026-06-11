package se.sundsvall.supportmanagement.api.model.handover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Options for handling the source errand after handover")
public class SourceHandling {

	@Schema(description = "Selectable statuses in the source namespace, used when choosing how the source errand is handled after handover. Always present, may be empty", requiredMode = REQUIRED)
	private List<MetadataOption> statusCandidates;

	public static SourceHandling create() {
		return new SourceHandling();
	}

	public List<MetadataOption> getStatusCandidates() {
		return statusCandidates;
	}

	public void setStatusCandidates(final List<MetadataOption> statusCandidates) {
		this.statusCandidates = statusCandidates;
	}

	public SourceHandling withStatusCandidates(final List<MetadataOption> statusCandidates) {
		this.statusCandidates = statusCandidates;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(statusCandidates);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final SourceHandling other)) {
			return false;
		}
		return Objects.equals(statusCandidates, other.statusCandidates);
	}

	@Override
	public String toString() {
		return "SourceHandling [statusCandidates=" + statusCandidates + "]";
	}
}
