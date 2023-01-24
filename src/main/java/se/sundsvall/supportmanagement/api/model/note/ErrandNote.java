package se.sundsvall.supportmanagement.api.model.note;

import java.time.OffsetDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "ErrandNote model")
public class ErrandNote {

	@Schema(description = "Note ID", example = "5f79a808-0ef3-4985-99b9-b12f23e202a7")
	private String id;

	@Schema(description = "Context for note", example = "SUPPORT")
	private String context;

	@Schema(description = "Role of note creator", example = "FIRST_LINE_SUPPORT")
	private String role;

	@Schema(description = "Id of the client who is the owner of the note", example = "SUPPORT_MGMT")
	private String clientId;

	@Schema(description = "Party ID (e.g. a personId or an organizationId)", example = "81471222-5798-11e9-ae24-57fa13b361e1")
	private String partyId;

	@Schema(description = "The note subject", example = "This is a subject")
	private String subject;

	@Schema(description = "The note body", example = "This is a note")
	private String body;

	@Schema(description = "Id for the case", example = "b82bd8ac-1507-4d9a-958d-369261eecc15")
	private String caseId;

	@Schema(description = "Created by", example = "John Doe")
	private String createdBy;

	@Schema(description = "Modified by", example = "John Doe")
	private String modifiedBy;

	@Schema(description = "Created timestamp")
	private OffsetDateTime created;

	@Schema(description = "Modified timestamp")
	private OffsetDateTime modified;

	public static ErrandNote create() {
		return new ErrandNote();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandNote withId(final String id) {
		this.id = id;
		return this;
	}

	public String getContext() {
		return context;
	}

	public void setContext(final String context) {
		this.context = context;
	}

	public ErrandNote withContext(final String context) {
		this.context = context;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public ErrandNote withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(final String clientId) {
		this.clientId = clientId;
	}

	public ErrandNote withClientId(final String clientId) {
		this.clientId = clientId;
		return this;
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public ErrandNote withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public ErrandNote withSubject(final String subject) {
		this.subject = subject;
		return this;
	}

	public String getBody() {
		return body;
	}

	public void setBody(final String body) {
		this.body = body;
	}

	public ErrandNote withBody(final String body) {
		this.body = body;
		return this;
	}

	public String getCaseId() {
		return caseId;
	}

	public void setCaseId(final String caseId) {
		this.caseId = caseId;
	}

	public ErrandNote withCaseId(final String caseId) {
		this.caseId = caseId;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public ErrandNote withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public ErrandNote withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ErrandNote withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public ErrandNote withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(body, caseId, clientId, context, created, createdBy, id, modified, modifiedBy, partyId, role, subject);
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
		final var other = (ErrandNote) obj;
		return Objects.equals(body, other.body) && Objects.equals(caseId, other.caseId) && Objects.equals(clientId, other.clientId) && Objects.equals(context, other.context) && Objects.equals(created, other.created) && Objects.equals(createdBy,
			other.createdBy) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(partyId, other.partyId) && Objects.equals(role, other.role) && Objects.equals(
				subject, other.subject);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("ErrandNote [id=").append(id).append(", context=").append(context).append(", role=").append(role).append(", clientId=").append(clientId).append(", partyId=").append(partyId).append(", subject=").append(subject).append(", body=")
			.append(body).append(", caseId=").append(caseId).append(", createdBy=").append(createdBy).append(", modifiedBy=").append(modifiedBy).append(", created=").append(created).append(", modified=").append(modified).append("]");
		return builder.toString();
	}
}
