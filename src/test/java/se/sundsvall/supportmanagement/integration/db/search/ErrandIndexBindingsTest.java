package se.sundsvall.supportmanagement.integration.db.search;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.model.enums.ErrandField;
import se.sundsvall.supportmanagement.model.enums.ProtectedResource;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandIndexBindingsTest {

	@Test
	void searchFieldsFollowThePropertyUnlessSaidOtherwise() {
		assertThat(ErrandIndexBindings.searchFields(ErrandField.TITLE)).containsExactly("title");
		assertThat(ErrandIndexBindings.searchFields(ErrandField.CLASSIFICATION)).containsExactly("category", "type");
		assertThat(ErrandIndexBindings.searchFields(ErrandField.SUSPENSION)).containsExactly("suspendedFrom", "suspendedTo");
		assertThat(ErrandIndexBindings.searchFields(ErrandField.STAKEHOLDERS)).containsExactly("stakeholders.");
		assertThat(ErrandIndexBindings.searchFields(ErrandField.JSON_PARAMETERS)).containsExactly("jsonParameters.", "jsonParametersText");
		assertThat(ErrandIndexBindings.searchFields(ErrandField.ID)).isEmpty();
		assertThat(ErrandIndexBindings.searchFields(ErrandField.VERSION)).isEmpty();
		assertThat(ErrandIndexBindings.searchFields(ErrandField.ACTIONS)).isEmpty();
		assertThat(ErrandIndexBindings.searchFields(ErrandField.ACTIVE_NOTIFICATIONS)).isEmpty();
	}

	@Test
	void sortsFollowTheBinding() {
		assertThat(ErrandIndexBindings.sortField(ErrandField.TITLE, "title")).contains("title_sort");
		assertThat(ErrandIndexBindings.sortableProperties(ErrandField.TITLE)).containsExactly("title");
		assertThat(ErrandIndexBindings.sortField(ErrandField.CLASSIFICATION, "category")).contains("category");
		assertThat(ErrandIndexBindings.sortField(ErrandField.CLASSIFICATION, "classification")).isEmpty();
		assertThat(ErrandIndexBindings.sortableProperties(ErrandField.CLASSIFICATION)).containsExactlyInAnyOrder("category", "type");
		assertThat(ErrandIndexBindings.sortField(ErrandField.DESCRIPTION, "description")).isEmpty();
		assertThat(ErrandIndexBindings.of(ErrandField.JSON_PARAMETERS).keysArePaths()).isTrue();
		assertThat(ErrandIndexBindings.of(ErrandField.PARAMETERS).keysArePaths()).isFalse();
	}

	/**
	 * A count groups by the single valued columns, and by them alone: a multi valued field would put an errand in several
	 * buckets and make the buckets add up to more than the count beside them.
	 */
	@Test
	void groupsFollowTheBinding() {
		assertThat(ErrandIndexBindings.groupField(ErrandField.STATUS, "status")).contains(ErrandIndex.STATUS);
		assertThat(ErrandIndexBindings.groupField(ErrandField.CLASSIFICATION, "category")).contains(ErrandIndex.CATEGORY);
		assertThat(ErrandIndexBindings.groupField(ErrandField.CLASSIFICATION, "type")).contains(ErrandIndex.TYPE);
		assertThat(ErrandIndexBindings.groupableProperties(ErrandField.CLASSIFICATION)).containsExactlyInAnyOrder("category", "type");

		// Ordered by, but not counted in groups of: a title is no category, and a date is no bucket
		assertThat(ErrandIndexBindings.groupableProperties(ErrandField.TITLE)).isEmpty();
		assertThat(ErrandIndexBindings.groupableProperties(ErrandField.CREATED)).isEmpty();
		assertThat(ErrandIndexBindings.groupableProperties(ErrandField.ERRAND_NUMBER)).isEmpty();
		assertThat(ErrandIndexBindings.groupableProperties(ErrandField.LABELS)).isEmpty();

		// Every group names a field the binding already holds, so nothing is grouped by a field nobody may search
		for (final var field : ErrandField.values()) {
			assertThat(ErrandIndexBindings.of(field).groups().values()).allSatisfy(name -> assertThat(ErrandIndexBindings.searchFields(field)).contains(name));
		}
	}

	@Test
	void onlyTheResourcesTheIndexHoldsHaveSearchFields() {
		assertThat(ErrandIndexBindings.searchFields(ProtectedResource.COMMUNICATION)).containsExactly("communications.");
		assertThat(ErrandIndexBindings.searchFields(ProtectedResource.JSON_PARAMETER)).containsExactly("jsonParameters.", "jsonParametersText");

		assertThat(Arrays.stream(ProtectedResource.values()).filter(resource -> !ErrandIndexBindings.searchFields(resource).isEmpty()))
			.allSatisfy(resource -> assertThat(resource.isErrandScoped()).isTrue())
			.doesNotContain(ProtectedResource.ERRAND, ProtectedResource.NOTE, ProtectedResource.CONVERSATION);
	}
}
