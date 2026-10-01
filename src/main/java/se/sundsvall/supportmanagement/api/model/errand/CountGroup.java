package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * How a count divides over one column of the errand.
 * <p>
 * The buckets add up to the count they are answered beside, always: every errand either carries a value in the column
 * and is counted under it, or carries none and is counted under the bucket whose value is null. A column holding more
 * values than a breakdown answers with is refused rather than shown in part, so there is no third case.
 *
 * @param property the property grouped by, as it is written in the payload of an errand
 * @param buckets  the values and their counts, the largest first, with the errands carrying no value last
 */
@Schema(description = "How a count divides over one column of the errand. The buckets add up to the count")
public record CountGroup(

	@Schema(description = "The property grouped by", example = "status") String property,

	@Schema(description = "The values and their counts, largest first. A bucket with a null value counts the errands holding nothing in the column") List<CountBucket> buckets) {
}
