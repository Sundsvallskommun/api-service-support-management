package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * How a count divides over one column of the errand.
 *
 * @param property  the property grouped by, as it is written in the payload of an errand
 * @param truncated true when the column holds more distinct values than the buckets returned, so that a partial
 *                  picture is never mistaken for the whole one
 * @param buckets   the values and their counts, the largest first
 */
@Schema(description = "How a count divides over one column of the errand")
public record CountGroup(

	@Schema(description = "The property grouped by", example = "status") String property,

	@Schema(description = "True when the column holds more distinct values than the buckets returned", example = "false") boolean truncated,

	@Schema(description = "The values and their counts, the largest first") List<CountBucket> buckets) {
}
