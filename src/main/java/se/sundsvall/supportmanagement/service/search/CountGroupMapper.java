package se.sundsvall.supportmanagement.service.search;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.api.model.errand.CountBucket;
import se.sundsvall.supportmanagement.api.model.errand.CountGroup;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.service.MetadataService;

import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Turns the buckets the index answers with into the breakdown a client reads.
 * <p>
 * Two things have to happen on the way. Every column a count groups by is indexed through a lowercasing normalizer, so
 * the index answers with {@code new} where the errand holds {@code NEW}, and the metadata of the namespace is what
 * knows the casing - except for the columns that are enumerations of this API, whose casing is known here. And the
 * index
 * counts only the errands that carry a value in the column at all, so the ones carrying none have to be counted back
 * in,
 * or the buckets would add up to less than the count they are answered beside.
 */
@Component
public class CountGroupMapper {

	private final MetadataService metadataService;

	public CountGroupMapper(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	/**
	 * @param counts   every value of the column and how many errands carry it, counted over the routes allowed to read
	 *                 it and already held to the number of buckets a breakdown answers with
	 * @param total    how many errands the query matched, which is what the breakdown is made to account for
	 * @param readable how many of those errands lie on a route allowed to read the column
	 */
	public CountGroup toGroup(final String property, final Map<String, Long> counts, final long total, final long readable, final String namespace, final String municipalityId) {
		final var canonical = canonicalNames(property, namespace, municipalityId, !counts.isEmpty());

		final var buckets = counts.entrySet().stream()
			.sorted(comparingByValue(reverseOrder()))
			.map(bucket -> new CountBucket(canonical.apply(bucket.getKey()), bucket.getValue()))
			.toList();

		// Every groupable column holds at most one value, so a bucket counts each errand once and what the buckets do not
		// account for among the readable errands is the errands carrying nothing. Subtracted rather than asked for, which
		// is exact only because a breakdown answered in part is refused before this
		final var bucketed = buckets.stream().mapToLong(CountBucket::count).sum();

		// And what lies outside the readable routes is withheld whole: not even the absence of a value is said of it, that
		// being a fact about the column as much as a value is
		// Never below nothing: where the routes differ the two counts are taken a moment apart, and an errand indexed in
		// between would otherwise answer with a negative number of errands
		return new CountGroup(property, buckets, Math.max(0, readable - bucketed), Math.max(0, total - readable));
	}

	/**
	 * The casing the namespace gives a value, by the value as the index holds it. A value the metadata does not know - a
	 * status configured away since the errand was given it - is answered with as the index holds it rather than left out,
	 * since it is errands of the count either way.
	 */
	private UnaryOperator<String> canonicalNames(final String property, final String namespace, final String municipalityId, final boolean anyBuckets) {
		if (!anyBuckets) {
			// Nothing to name, and the metadata is a database query rather than a cache: not asked for where the answer is
			// an empty breakdown
			return UnaryOperator.identity();
		}

		final var names = switch (property) {
			case "status" -> metadataService.findStatuses(namespace, municipalityId, Sort.unsorted()).stream().map(Status::getName);
			case "category" -> metadataService.findCategories(namespace, municipalityId, Sort.unsorted()).stream().map(Category::getName);
			case "type" -> metadataService.findCategories(namespace, municipalityId, Sort.unsorted()).stream()
				.map(Category::getTypes)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.map(Type::getName);
			// A closed enum of the API, so the casing is known here and needs no namespace to be asked. Without this the
			// index answered 'high' where the model declares HIGH, which is a value no client can read back into Priority
			case "priority" -> Stream.of(Priority.values()).map(Priority::name);
			// What is left is identifiers and the uncatalogued strings: answered as the errand holds them, lowercased by
			// the index, which is as close as the index can come to the value that was written
			default -> Stream.<String>empty();
		};

		final var byLowercase = names.filter(Objects::nonNull)
			.collect(toMap(name -> name.toLowerCase(Locale.ROOT), identity(), (first, second) -> first));
		return value -> byLowercase.getOrDefault(value, value);
	}
}
