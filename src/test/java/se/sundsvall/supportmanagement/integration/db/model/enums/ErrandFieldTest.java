package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Errand;

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
}
