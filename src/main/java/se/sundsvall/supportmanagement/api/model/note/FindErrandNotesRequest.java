package se.sundsvall.supportmanagement.api.model.note;

import static java.lang.Integer.parseInt;

import java.util.Objects;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import io.swagger.v3.oas.annotations.media.Schema;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "FindErrandNotesRequest model")
public class FindErrandNotesRequest {

	private static final String DEFAULT_PAGE = "1";
	private static final String DEFAULT_LIMIT = "100";

	@Schema(description = "Context for note", example = "SUPPORT")
	private String context;

	@Schema(description = "Role of note creator", example = "FIRST_LINE_SUPPORT")
	private String role;

	@Schema(description = "Party id (e.g. a personId or an organizationId)", example = "81471222-5798-11e9-ae24-57fa13b361e1")
	@ValidUuid(nullable = true)
	private String partyId;

	@Schema(description = "Page number", example = DEFAULT_PAGE, defaultValue = DEFAULT_PAGE)
	@Min(1)
	protected int page = parseInt(DEFAULT_PAGE);

	@Schema(description = "Result size per page", example = DEFAULT_LIMIT, defaultValue = DEFAULT_LIMIT)
	@Min(1)
	@Max(1000)
	protected int limit = parseInt(DEFAULT_LIMIT);

	public static FindErrandNotesRequest create() {
		return new FindErrandNotesRequest();
	}

	public String getContext() {
		return context;
	}

	public void setContext(final String context) {
		this.context = context;
	}

	public FindErrandNotesRequest withContext(final String context) {
		this.context = context;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public FindErrandNotesRequest withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public FindErrandNotesRequest withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public int getPage() {
		return page;
	}

	public void setPage(final int page) {
		this.page = page;
	}

	public FindErrandNotesRequest withPage(final int page) {
		this.page = page;
		return this;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(final int limit) {
		this.limit = limit;
	}

	public FindErrandNotesRequest withLimit(final int limit) {
		this.limit = limit;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(context, limit, page, partyId, role);
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
		final var other = (FindErrandNotesRequest) obj;
		return Objects.equals(context, other.context) && limit == other.limit && page == other.page && Objects.equals(partyId, other.partyId) && Objects.equals(role, other.role);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("FindErrandNotesRequest [context=").append(context).append(", role=").append(role).append(", partyId=").append(partyId).append(", page=").append(page).append(", limit=").append(limit).append("]");
		return builder.toString();
	}

}
