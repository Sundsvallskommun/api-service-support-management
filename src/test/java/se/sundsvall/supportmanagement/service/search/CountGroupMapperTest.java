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
 * The index answers a grouped count with lowercased values and one bucket more than is answered with; this is what the
 * client reads instead.
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

		final var group = mapper.toGroup("status", counts("ongoing", 46, "new", 91), NAMESPACE, MUNICIPALITY_ID, 100);

		assertThat(group.property()).isEqualTo("status");
		assertThat(group.truncated()).isFalse();
		assertThat(group.buckets()).containsExactly(new CountBucket("NEW", 91), new CountBucket("ONGOING", 46));
	}

	/**
	 * A status configured away since an errand was given it is still errands of the count, so it is answered with as the
	 * index holds it rather than left out.
	 */
	@Test
	void aValueTheMetadataDoesNotKnowIsAnsweredWithAsTheIndexHoldsIt() {
		when(metadataServiceMock.findStatuses(eq(NAMESPACE), eq(MUNICIPALITY_ID), any())).thenReturn(List.of(Status.create().withName("NEW")));

		final var group = mapper.toGroup("status", counts("new", 3, "retired_status", 1), NAMESPACE, MUNICIPALITY_ID, 100);

		assertThat(group.buckets()).containsExactly(new CountBucket("NEW", 3), new CountBucket("retired_status", 1));
	}

	/**
	 * The index is asked for one bucket more than is answered with, so more than the cap means the column holds values
	 * the breakdown does not show.
	 */
	@Test
	void moreBucketsThanTheCapSaysSoAndAnswersWithTheLargest() {
		final var group = mapper.toGroup("assignedUserId", counts("a", 9, "b", 8, "c", 7), NAMESPACE, MUNICIPALITY_ID, 2);

		assertThat(group.truncated()).isTrue();
		assertThat(group.buckets()).containsExactly(new CountBucket("a", 9), new CountBucket("b", 8));
		// Nothing catalogues an ad account, so the metadata is not asked
		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void exactlyTheCapIsNotTruncated() {
		final var group = mapper.toGroup("assignedUserId", counts("a", 9, "b", 8), NAMESPACE, MUNICIPALITY_ID, 2);

		assertThat(group.truncated()).isFalse();
		assertThat(group.buckets()).hasSize(2);
	}

	@Test
	void nothingMatchedIsAnEmptyBreakdownRatherThanNone() {
		final var group = mapper.toGroup("assignedUserId", Map.of(), NAMESPACE, MUNICIPALITY_ID, 100);

		assertThat(group.truncated()).isFalse();
		assertThat(group.buckets()).isEmpty();
	}

	/** The types of a namespace hang under its categories, so both come from the one lookup. */
	@Test
	void theCasingOfACategoryAndOfATypeBothComeFromTheCategories() {
		when(metadataServiceMock.findCategories(eq(NAMESPACE), eq(MUNICIPALITY_ID), any())).thenReturn(List.of(
			Category.create().withName("SUPPORT-CASE").withTypes(List.of(Type.create().withName("OTHER_ISSUES"))),
			Category.create().withName("NO-TYPES")));

		assertThat(mapper.toGroup("category", counts("support-case", 2), NAMESPACE, MUNICIPALITY_ID, 100).buckets())
			.containsExactly(new CountBucket("SUPPORT-CASE", 2));
		assertThat(mapper.toGroup("type", counts("other_issues", 5), NAMESPACE, MUNICIPALITY_ID, 100).buckets())
			.containsExactly(new CountBucket("OTHER_ISSUES", 5));
	}
}
