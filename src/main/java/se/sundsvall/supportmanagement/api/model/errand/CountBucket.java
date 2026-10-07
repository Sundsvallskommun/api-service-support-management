package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One value of the column a count is grouped by, and how many errands carry it.
 * <p>
 * Only values: the errands carrying none, and those carrying one the user may not read, are counted by
 * {@link CountGroup#withoutValue()} and {@link CountGroup#withheld()} rather than given a bucket of their own.
 *
 * @param value the value, in the casing the metadata of the namespace gives it where it knows it
 * @param count how many of the matching errands carry it
 */
@Schema(description = "One value of the column grouped by, and how many errands carry it")
public record CountBucket(

	@Schema(description = "The value", example = "NEW") String value,

	@Schema(description = "How many of the matching errands carry it", example = "91") long count) {
}
