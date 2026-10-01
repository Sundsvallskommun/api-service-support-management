package se.sundsvall.supportmanagement.service.search;

import java.util.ArrayList;
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
 * knows the casing. And the index counts only the errands that carry a value in the column at all, so the ones carrying
 * none have to be counted back in, or the buckets would add up to less than the count they are answered beside.
 */
@Component
public class CountGroupMapper {

	private final MetadataService metadataService;

	public CountGroupMapper(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	/**
	 * @param counts every value of the column and how many errands carry it, which the caller has already held to the
	 *               number of buckets a breakdown answers with
	 * @param total  how many errands the query matched, which is what the buckets are made to add up to
	 */
	public CountGroup toGroup(final String property, final Map<String, Long> counts, final long total, final String namespace, final String municipalityId) {
		final var canonical = canonicalNames(property, namespace, municipalityId);

		final var buckets = new ArrayList<CountBucket>(counts.entrySet().stream()
			.sorted(comparingByValue(reverseOrder()))
			.map(bucket -> new CountBucket(canonical.apply(bucket.getKey()), bucket.getValue()))
			.toList());

		// What the index did not count: a terms aggregation sees only the errands holding a value, and every column a count
		// may group by holds at most one, so whatever the buckets do not add up to is the errands holding none. Subtracted
		// rather than asked for, which is exact only because a breakdown answered in part is refused before this
		final var withoutValue = total - buckets.stream().mapToLong(CountBucket::count).sum();
		if (withoutValue > 0) {
			buckets.add(new CountBucket(null, withoutValue));
		}

		return new CountGroup(property, List.copyOf(buckets));
	}

	/**
	 * The casing the namespace gives a value, by the value as the index holds it. A value the metadata does not know - a
	 * status configured away since the errand was given it - is answered with as the index holds it rather than left out,
	 * since it is errands of the count either way.
	 */
	private UnaryOperator<String> canonicalNames(final String property, final String namespace, final String municipalityId) {
		final var names = switch (property) {
			case "status" -> metadataService.findStatuses(namespace, municipalityId, Sort.unsorted()).stream().map(Status::getName);
			case "category" -> metadataService.findCategories(namespace, municipalityId, Sort.unsorted()).stream().map(Category::getName);
			case "type" -> metadataService.findCategories(namespace, municipalityId, Sort.unsorted()).stream()
				.map(Category::getTypes)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.map(Type::getName);
			// The identifiers and the enumerations nobody catalogues: what was written, lowercased by the index
			default -> Stream.<String>empty();
		};

		final var byLowercase = names.filter(Objects::nonNull)
			.collect(toMap(name -> name.toLowerCase(Locale.ROOT), identity(), (first, second) -> first));
		return value -> byLowercase.getOrDefault(value, value);
	}
}
