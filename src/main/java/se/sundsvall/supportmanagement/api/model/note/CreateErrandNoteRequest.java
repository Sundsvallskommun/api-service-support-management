package se.sundsvall.supportmanagement.api.model.note;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "CreateErrandNoteRequest model")
public class CreateErrandNoteRequest {

	@Schema(description = "Context for note", example = "SUPPORT", minLength = 1, maxLength = 255, requiredMode = REQUIRED)
	@NotBlank
	@Size(min = 1, max = 255)
	private String context;

	@Schema(description = "Role of note creator", example = "FIRST_LINE_SUPPORT", minLength = 1, maxLength = 255, requiredMode = REQUIRED)
	@NotBlank
	@Size(min = 1, max = 255)
	private String role;

	@Schema(description = "Party id (e.g. a personId or an organizationId)", example = "81471222-5798-11e9-ae24-57fa13b361e1", requiredMode = NOT_REQUIRED)
	@ValidUuid(nullable = true)
	private String partyId;

	@Schema(description = "The note subject", example = "This is a subject", minLength = 1, maxLength = 255, requiredMode = REQUIRED)
	@NotBlank
	@Size(min = 1, max = 255)
	private String subject;

	@Schema(description = "The note body", example = "This is a note", minLength = 1, maxLength = 2048, requiredMode = REQUIRED)
	@NotBlank
	@Size(min = 1, max = 2048)
	private String body;

	@Schema(description = "Created by", example = "John Doe", requiredMode = REQUIRED)
	@NotBlank
	private String createdBy;

	public static CreateErrandNoteRequest create() {
		return new CreateErrandNoteRequest();
	}

	public String getContext() {
		return context;
	}

	public void setContext(final String context) {
		this.context = context;
	}

	public CreateErrandNoteRequest withContext(final String context) {
		this.context = context;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public CreateErrandNoteRequest withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public CreateErrandNoteRequest withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public CreateErrandNoteRequest withSubject(final String subject) {
		this.subject = subject;
		return this;
	}

	public String getBody() {
		return body;
	}

	public void setBody(final String body) {
		this.body = body;
	}

	public CreateErrandNoteRequest withBody(final String body) {
		this.body = body;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public CreateErrandNoteRequest withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(body, context, createdBy, partyId, role, subject);
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
		final var other = (CreateErrandNoteRequest) obj;
		return Objects.equals(body, other.body) && Objects.equals(context, other.context) && Objects.equals(createdBy, other.createdBy) && Objects.equals(partyId, other.partyId) && Objects.equals(role, other.role) && Objects.equals(subject, other.subject);
	}

	@Override
	public String toString() {
		return "CreateErrandNoteRequest [context=" + context + ", role=" + role + ", partyId=" + partyId + ", subject=" + subject + ", body=" + body + ", createdBy=" + createdBy
			+ "]";
	}
}
