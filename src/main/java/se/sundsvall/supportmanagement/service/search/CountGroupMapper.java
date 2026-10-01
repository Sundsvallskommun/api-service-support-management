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
 * knows the casing. And a column with no bounded set of values is answered with in part, so a breakdown that left
 * something out has to say so rather than pass for the whole picture.
 */
@Component
public class CountGroupMapper {

	private final MetadataService metadataService;

	public CountGroupMapper(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	/**
	 * @param counts     the buckets the index answered with, which hold one more than is answered with where there was
	 *                   one more
	 * @param maxBuckets how many buckets to answer with
	 */
	public CountGroup toGroup(final String property, final Map<String, Long> counts, final String namespace, final String municipalityId, final int maxBuckets) {
		final var truncated = counts.size() > maxBuckets;
		final var canonical = canonicalNames(property, namespace, municipalityId);

		final var buckets = counts.entrySet().stream()
			.sorted(comparingByValue(reverseOrder()))
			.limit(maxBuckets)
			.map(bucket -> new CountBucket(canonical.apply(bucket.getKey()), bucket.getValue()))
			.toList();

		return new CountGroup(property, truncated, buckets);
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
