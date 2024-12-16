package se.sundsvall.supportmanagement.api.model.note;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

@Schema(description = "UpdateErrandNoteRequest model")
public class UpdateErrandNoteRequest {

	@Schema(description = "The note subject", example = "This is a subject", maxLength = 255, requiredMode = REQUIRED)
	@NotBlank
	@Size(min = 1, max = 255)
	private String subject;

	@Schema(description = "The note body", example = "This is a note", maxLength = 2048, requiredMode = REQUIRED)
	@NotBlank
	@Size(min = 1, max = 2048)
	private String body;

	@Schema(description = "Modified by", example = "John Doe", requiredMode = REQUIRED)
	@NotBlank
	private String modifiedBy;

	public static UpdateErrandNoteRequest create() {
		return new UpdateErrandNoteRequest();
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public UpdateErrandNoteRequest withSubject(final String subject) {
		this.subject = subject;
		return this;
	}

	public String getBody() {
		return body;
	}

	public void setBody(final String body) {
		this.body = body;
	}

	public UpdateErrandNoteRequest withBody(final String body) {
		this.body = body;
		return this;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public UpdateErrandNoteRequest withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(body, modifiedBy, subject);
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
		final var other = (UpdateErrandNoteRequest) obj;
		return Objects.equals(body, other.body) && Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(subject, other.subject);
	}

	@Override
	public String toString() {
		return "UpdateErrandNoteRequest [subject=" + subject + ", body=" + body + ", modifiedBy=" + modifiedBy + "]";
	}
}
