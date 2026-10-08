package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.search.ErrandIndex;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

class ErrandFieldTest {

	/**
	 * Verifies that the property every field names exists on the errand.
	 */
	@Test
	void everyFieldNamesAPropertyOfTheErrand() {
		final Set<String> properties = Arrays.stream(Errand.class.getDeclaredFields())
			.filter(field -> !field.isSynthetic())
			.filter(field -> !Modifier.isStatic(field.getModifiers()))
			.map(Field::getName)
			.collect(toSet());

		assertThat(Arrays.stream(ErrandField.values()).map(ErrandField::getPropertyName))
			.isNotEmpty()
			.allSatisfy(propertyName -> assertThat(properties).contains(propertyName));
	}

	/**
	 * Verifies that the property every field names is the name of its constant in camel case.
	 */
	@Test
	void everyFieldNamesThePropertyItsConstantCorrespondsTo() {
		assertThat(ErrandField.values())
			.allSatisfy(field -> assertThat(field.getPropertyName()).isEqualTo(camelCased(field.name())));
	}

	private static String camelCased(final String constant) {
		final var parts = constant.toLowerCase(Locale.ROOT).split("_");
		return IntStream.range(0, parts.length)
			.mapToObj(i -> i == 0 ? parts[i] : parts[i].substring(0, 1).toUpperCase(Locale.ROOT) + parts[i].substring(1))
			.collect(joining());
	}

	/**
	 * Verifies that only the parameters and the JSON parameters, keyed fields served by an endpoint of their own, carry a
	 * write resource.
	 */
	@Test
	void onlyTheKeyedFieldsServedByAnEndpointCarryAWriteResource() {
		assertThat(Arrays.stream(ErrandField.values()).filter(field -> field.getWriteResource() != null))
			.containsExactlyInAnyOrder(ErrandField.PARAMETERS, ErrandField.JSON_PARAMETERS);

		assertThat(ErrandField.PARAMETERS.getWriteResource()).isEqualTo(ProtectedResource.PARAMETER);
		assertThat(ErrandField.JSON_PARAMETERS.getWriteResource()).isEqualTo(ProtectedResource.JSON_PARAMETER);
		assertThat(Arrays.stream(ErrandField.values()).filter(field -> field.getWriteResource() != null))
			.allSatisfy(field -> assertThat(field.isKeyed()).isTrue());
	}

	@Test
	void everyFieldCarriesADistinctProperty() {
		assertThat(Arrays.stream(ErrandField.values()).map(ErrandField::getPropertyName).collect(toSet()))
			.hasSize(ErrandField.values().length);
	}

	@Test
	void onlyTheCollectionsCarryingAKeyAreKeyed() {
		assertThat(Arrays.stream(ErrandField.values()).filter(ErrandField::isKeyed))
			.containsExactly(ErrandField.PARAMETERS, ErrandField.JSON_PARAMETERS, ErrandField.EXTERNAL_TAGS);
	}

	/**
	 * Properties of the errand that no field names: only the phase a request names to move the errand into, which no
	 * response carries.
	 */
	private static final Set<String> UNRESTRICTABLE = Set.of("activePhaseId");

	/**
	 * Verifies that every property of the errand is named by a field or listed in {@link #UNRESTRICTABLE}.
	 */
	@Test
	void everyPropertyOfTheErrandIsNamedByAFieldOrDeliberatelyNot() {
		final var named = Arrays.stream(ErrandField.values())
			.map(ErrandField::getPropertyName)
			.collect(toSet());

		assertThat(Arrays.stream(Errand.class.getDeclaredFields())
			.filter(field -> !field.isSynthetic())
			.filter(field -> !Modifier.isStatic(field.getModifiers()))
			.map(Field::getName)
			.filter(property -> !named.contains(property)))
			.as("properties of the errand that no ErrandField names")
			.containsExactlyInAnyOrderElementsOf(UNRESTRICTABLE);
	}

	@Test
	void searchFieldsFollowThePropertyUnlessSaidOtherwise() {
		assertThat(ErrandField.TITLE.getSearchFields()).containsExactly("title");
		assertThat(ErrandField.CLASSIFICATION.getSearchFields()).containsExactly("category", "type");
		assertThat(ErrandField.SUSPENSION.getSearchFields()).containsExactly("suspendedFrom", "suspendedTo");
		assertThat(ErrandField.STAKEHOLDERS.getSearchFields()).containsExactly("stakeholders.");
		assertThat(ErrandField.JSON_PARAMETERS.getSearchFields()).containsExactly("jsonParameters.", "jsonParametersText");
		assertThat(ErrandField.ID.getSearchFields()).isEmpty();
		assertThat(ErrandField.VERSION.getSearchFields()).isEmpty();
		assertThat(ErrandField.ACTIONS.getSearchFields()).isEmpty();
		assertThat(ErrandField.ACTIVE_NOTIFICATIONS.getSearchFields()).isEmpty();
	}

	@Test
	void sortsFollowTheBinding() {
		assertThat(ErrandField.TITLE.getSortField("title")).contains("title_sort");
		assertThat(ErrandField.TITLE.getSortableProperties()).containsExactly("title");
		assertThat(ErrandField.CLASSIFICATION.getSortField("category")).contains("category");
		assertThat(ErrandField.CLASSIFICATION.getSortField("classification")).isEmpty();
		assertThat(ErrandField.CLASSIFICATION.getSortableProperties()).containsExactlyInAnyOrder("category", "type");
		assertThat(ErrandField.DESCRIPTION.getSortField("description")).isEmpty();
		assertThat(ErrandField.JSON_PARAMETERS.getIndex().keysArePaths()).isTrue();
		assertThat(ErrandField.PARAMETERS.getIndex().keysArePaths()).isFalse();
	}

	/**
	 * A count groups by the single valued columns, and by them alone: a multi valued field would put an errand in several
	 * buckets and make the buckets add up to more than the count beside them.
	 */
	@Test
	void groupsFollowTheBinding() {
		assertThat(ErrandField.STATUS.getGroupField("status")).contains(ErrandIndex.STATUS);
		assertThat(ErrandField.CLASSIFICATION.getGroupField("category")).contains(ErrandIndex.CATEGORY);
		assertThat(ErrandField.CLASSIFICATION.getGroupField("type")).contains(ErrandIndex.TYPE);
		assertThat(ErrandField.CLASSIFICATION.getGroupableProperties()).containsExactlyInAnyOrder("category", "type");

		// Ordered by, but not counted in groups of: a title is no category, and a date is no bucket
		assertThat(ErrandField.TITLE.getGroupableProperties()).isEmpty();
		assertThat(ErrandField.CREATED.getGroupableProperties()).isEmpty();
		assertThat(ErrandField.ERRAND_NUMBER.getGroupableProperties()).isEmpty();
		assertThat(ErrandField.LABELS.getGroupableProperties()).isEmpty();

		// Every group names a field the binding already holds, so nothing is grouped by a field nobody may search
		for (final var field : ErrandField.values()) {
			assertThat(field.getIndex().groups().values()).allSatisfy(name -> assertThat(field.getSearchFields()).contains(name));
		}
	}
}
