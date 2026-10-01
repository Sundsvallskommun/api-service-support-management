package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How many errands a search matches, and optionally how that number divides over one column of the errand.
 *
 * @param count how many errands the query matches, as the index answers it
 * @param group the breakdown asked for, null where none was
 */
@Schema(description = "How many errands a search matches, and optionally how that number divides over one column")
public record SearchCountResponse(

	@Schema(description = "Number of matching errands", example = "137") long count,

	@Schema(description = "The breakdown, absent unless a grouping was asked for") CountGroup group) {

	public static SearchCountResponse of(final long count) {
		return new SearchCountResponse(count, null);
	}
}
