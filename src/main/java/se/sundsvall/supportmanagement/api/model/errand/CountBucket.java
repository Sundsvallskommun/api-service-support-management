package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One value of the column a count is grouped by, and how many errands carry it.
 *
 * @param value the value, in the casing the metadata of the namespace gives it where it knows it
 * @param count how many of the matching errands carry it
 */
@Schema(description = "One value of the column grouped by, and how many errands carry it")
public record CountBucket(

	@Schema(description = "The value", example = "NEW") String value,

	@Schema(description = "How many of the matching errands carry it", example = "91") long count) {
}
