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
	 * The property of every field has to exist on the errand, since the API reports it in place of the constant and a
	 * client looks it up in the payload. Renaming a property of the errand without renaming it here would leave the
	 * report pointing at something that is not there.
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
	 * Existing on the errand is not enough on its own - a field naming the wrong property of it would satisfy that and
	 * still report the access of one field under the name of another. The property is written out on the constant rather
	 * than derived from it so that renaming a property of the errand cannot quietly rename it in the published contract,
	 * and this holds the two to each other in the ordinary case where they correspond.
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
	 * Properties of the errand deliberately left unrestrictable. They are served to everyone reaching the errand and
	 * carry no grant of their own, which is why no field names them.
	 */
	private static final Set<String> UNRESTRICTABLE = Set.of("phases", "activePhaseId", "actions");

	/**
	 * The direction the other tests do not cover: a property added to the errand has to be named by a field, or named
	 * here as one that is deliberately not.
	 * <p>
	 * An errand is built from the mappers of these fields, so a property no field names is not merely unrestricted - it
	 * is never written at all, and would be absent from every response rather than from a restricted one. Adding a
	 * property therefore has to fail here until the field, and with it the mapper, is added too.
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
