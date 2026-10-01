package se.sundsvall.supportmanagement.api.model.errand;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One value of the column a count is grouped by, and how many errands carry it.
 *
 * @param value the value, in the casing the metadata of the namespace gives it where it knows it, or null for the
 *              errands carrying nothing in the column
 * @param count how many of the matching errands carry it
 */
// The null value is the answer, not a missing one - it counts the errands holding nothing in the column - so it is
// written out rather than dropped by the NON_NULL inclusion the service sets for everything else
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(description = "One value of the column grouped by, and how many errands carry it")
public record CountBucket(

	@Schema(description = "The value, or null for the errands holding nothing in the column", example = "NEW") String value,

	@Schema(description = "How many of the matching errands carry it", example = "91") long count) {
}
