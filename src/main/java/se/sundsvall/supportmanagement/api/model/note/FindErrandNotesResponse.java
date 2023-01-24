package se.sundsvall.supportmanagement.api.model.note;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import se.sundsvall.supportmanagement.api.model.MetaData;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "FindErrandNotesResponse model", accessMode = READ_ONLY)
public class FindErrandNotesResponse {

	@JsonProperty("_meta")
	@Schema(implementation = MetaData.class)
	private MetaData metaData;

	@ArraySchema(schema = @Schema(implementation = ErrandNote.class))
	private List<ErrandNote> notes;

	public static FindErrandNotesResponse create() {
		return new FindErrandNotesResponse();
	}

	public MetaData getMetaData() {
		return metaData;
	}

	public void setMetaData(final MetaData metaData) {
		this.metaData = metaData;
	}

	public FindErrandNotesResponse withMetaData(final MetaData metaData) {
		this.metaData = metaData;
		return this;
	}

	public List<ErrandNote> getNotes() {
		return notes;
	}

	public void setNotes(final List<ErrandNote> notes) {
		this.notes = notes;
	}

	public FindErrandNotesResponse withNotes(final List<ErrandNote> notes) {
		this.notes = notes;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(metaData, notes);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (FindErrandNotesResponse) obj;
		return Objects.equals(metaData, other.metaData) && Objects.equals(notes, other.notes);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("FindErrandNotesResponse [metaData=").append(metaData).append(", notes=").append(notes).append("]");
		return builder.toString();
	}
}
