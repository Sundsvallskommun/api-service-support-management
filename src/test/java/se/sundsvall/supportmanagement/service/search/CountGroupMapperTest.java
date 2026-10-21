package se.sundsvall.supportmanagement.service.search;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.errand.CountBucket;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The index answers a grouped count with lowercased values, and counts only the errands that carry a value at all; this
 * is what the client reads instead. The one invariant throughout: the buckets add up to the count they are answered
 * beside.
 */
@ExtendWith(MockitoExtension.class)
class CountGroupMapperTest {

	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private MetadataService metadataServiceMock;

	@InjectMocks
	private CountGroupMapper mapper;

	/** What a terms aggregation answers with: counts by the lowercased value, in no order worth relying on. */
	private static Map<String, Long> counts(final Object... pairs) {
		final var counts = new LinkedHashMap<String, Long>();
		for (var i = 0; i < pairs.length; i += 2) {
			counts.put((String) pairs[i], ((Number) pairs[i + 1]).longValue());
		}
		return counts;
	}

	@Test
	void theLargestBucketComesFirstAndTheCasingComesFromTheMetadata() {
		when(metadataServiceMock.findStatuses(eq(NAMESPACE), eq(MUNICIPALITY_ID), any())).thenReturn(List.of(
			Status.create().withName("NEW"), Status.create().withName("ONGOING"), Status.create().withName("SOLVED")));

		final var group = mapper.toGroup("status", counts("ongoing", 46, "new", 91), 137, 137, NAMESPACE, MUNICIPALITY_ID);

		assertThat(group.property()).isEqualTo("status");
		assertThat(group.buckets()).containsExactly(new CountBucket("NEW", 91), new CountBucket("ONGOING", 46));
	}

	/**
	 * A status configured away since an errand was given it is still errands of the count, so it is answered with as the
	 * index holds it rather than left out.
	 */
	@Test
	void aValueTheMetadataDoesNotKnowIsAnsweredWithAsTheIndexHoldsIt() {
		when(metadataServiceMock.findStatuses(eq(NAMESPACE), eq(MUNICIPALITY_ID), any())).thenReturn(List.of(Status.create().withName("NEW")));

		final var group = mapper.toGroup("status", counts("new", 3, "retired_status", 1), 4, 4, NAMESPACE, MUNICIPALITY_ID);

		assertThat(group.buckets()).containsExactly(new CountBucket("NEW", 3), new CountBucket("retired_status", 1));
	}

	/**
	 * The index counts only the errands carrying a value, so the rest are accounted for beside the buckets rather than
	 * going missing from a breakdown printed next to their count.
	 */
	@Test
	void theErrandsCarryingNothingAreCountedBesideTheBuckets() {
		final var group = mapper.toGroup("assignedUserId", counts("han01dle", 9, "han02dle", 3), 137, 137, NAMESPACE, MUNICIPALITY_ID);

		assertThat(group.buckets()).containsExactly(new CountBucket("han01dle", 9), new CountBucket("han02dle", 3));
		assertThat(group.withoutValue()).isEqualTo(125);
		assertThat(group.withheld()).isZero();
		assertThat(accountedFor(group)).isEqualTo(137);
		// Nothing catalogues an ad account, so the metadata is not asked
		verifyNoInteractions(metadataServiceMock);
	}

	/**
	 * Errands on a route that may not read the column are withheld whole, and told apart from the errands carrying
	 * nothing: that an errand holds no value is a fact about the column as much as a value is.
	 */
	@Test
	void theErrandsOnARouteThatMayNotReadTheColumnAreWithheld() {
		final var group = mapper.toGroup("assignedUserId", counts("han01dle", 9), 137, 12, NAMESPACE, MUNICIPALITY_ID);

		assertThat(group.buckets()).containsExactly(new CountBucket("han01dle", 9));
		assertThat(group.withoutValue()).isEqualTo(3);
		assertThat(group.withheld()).isEqualTo(125);
		assertThat(accountedFor(group)).isEqualTo(137);
	}

	/** Where no route may read it, every errand is withheld and none is said to carry nothing. */
	@Test
	void whereNothingIsReadableEverythingIsWithheld() {
		final var group = mapper.toGroup("assignedUserId", Map.of(), 42, 0, NAMESPACE, MUNICIPALITY_ID);

		assertThat(group.buckets()).isEmpty();
		assertThat(group.withoutValue()).isZero();
		assertThat(group.withheld()).isEqualTo(42);
	}

	/** A column every errand carries leaves nothing beside the buckets. */
	@Test
	void nothingIsLeftWhenEveryErrandCarriesAValue() {
		final var group = mapper.toGroup("assignedUserId", counts("a", 9, "b", 8), 17, 17, NAMESPACE, MUNICIPALITY_ID);

		assertThat(group.buckets()).containsExactly(new CountBucket("a", 9), new CountBucket("b", 8));
		assertThat(group.withoutValue()).isZero();
		assertThat(group.withheld()).isZero();
	}

	/** A column no errand carries is one number rather than a bucket. */
	@Test
	void aColumnNobodyCarriesIsCountedAsCarryingNothing() {
		final var group = mapper.toGroup("resolution", Map.of(), 42, 42, NAMESPACE, MUNICIPALITY_ID);

		assertThat(group.buckets()).isEmpty();
		assertThat(group.withoutValue()).isEqualTo(42);
	}

	@Test
	void nothingMatchedIsAnEmptyBreakdown() {
		final var group = mapper.toGroup("assignedUserId", Map.of(), 0, 0, NAMESPACE, MUNICIPALITY_ID);

		assertThat(group.buckets()).isEmpty();
		assertThat(group.withoutValue()).isZero();
		assertThat(group.withheld()).isZero();
	}

	/** What the breakdown accounts for, which must always be the count it was answered beside. */
	private static long accountedFor(final se.sundsvall.supportmanagement.api.model.errand.CountGroup group) {
		return group.buckets().stream().mapToLong(CountBucket::count).sum() + group.withoutValue() + group.withheld();
	}

	/** The types of a namespace hang under its categories, so both come from the one lookup. */
	@Test
	void theCasingOfACategoryAndOfATypeBothComeFromTheCategories() {
		when(metadataServiceMock.findCategories(eq(NAMESPACE), eq(MUNICIPALITY_ID), any())).thenReturn(List.of(
			Category.create().withName("SUPPORT-CASE").withTypes(List.of(Type.create().withName("OTHER_ISSUES"))),
			Category.create().withName("NO-TYPES")));

		assertThat(mapper.toGroup("category", counts("support-case", 2), 2, 2, NAMESPACE, MUNICIPALITY_ID).buckets())
			.containsExactly(new CountBucket("SUPPORT-CASE", 2));
		assertThat(mapper.toGroup("type", counts("other_issues", 5), 5, 5, NAMESPACE, MUNICIPALITY_ID).buckets())
			.containsExactly(new CountBucket("OTHER_ISSUES", 5));
	}
}
