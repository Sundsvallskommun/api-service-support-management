package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * How a count divides over one column of the errand.
 * <p>
 * It accounts for every errand the count counted: {@code buckets} plus {@code withoutValue} plus {@code withheld} is
 * the count. That matters because the count is the count of the search however it is divided up - a client filters with
 * a search and asks for the breakdown of the same filter, so the two may not disagree on what was counted.
 * <p>
 * The two numbers beside the buckets are different facts and are kept apart. An errand may carry nothing in the column,
 * which is {@code withoutValue}. Or it may carry something the user is not allowed to read, which is {@code withheld} -
 * those errands are counted and nothing more is said of them, not even whether they carry a value, since that would be
 * a fact about the column too.
 *
 * @param property     the property grouped by, as it is written in the payload of an errand
 * @param buckets      the values and their counts, the largest first
 * @param withoutValue how many of the counted errands carry nothing in the column
 * @param withheld     how many of the counted errands carry a column this user may not read
 */
@Schema(description = "How a count divides over one column of the errand. The buckets, withoutValue and withheld together account for the count")
public record CountGroup(

	@Schema(description = "The property grouped by", example = "status") String property,

	@Schema(description = "The values and their counts, largest first") List<CountBucket> buckets,

	@Schema(description = "How many of the counted errands carry nothing in the column", example = "3") long withoutValue,

	@Schema(description = "How many of the counted errands carry a column this user may not read", example = "0") long withheld) {
}
