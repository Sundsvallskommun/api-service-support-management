package se.sundsvall.supportmanagement.api.validation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.metadata.LabelAttribute;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ValidLabelAttributesConstraintValidatorTest {

	private final ValidLabelAttributesConstraintValidator validator = new ValidLabelAttributesConstraintValidator();

	@Test
	void attributesWithUniqueKeys() {
		assertThat(validator.isValid(List.of(
			attribute("escalationEmail", "a@example.com"),
			attribute("owner", "team-a")), null)).isTrue();
	}

	@Test
	void attributesWithDuplicateKeys() {
		assertThat(validator.isValid(List.of(
			attribute("escalationEmail", "a@example.com"),
			attribute("escalationEmail", "b@example.com")), null)).isFalse();
	}

	@Test
	void keysDifferingOnlyInCaseAreUnique() {
		assertThat(validator.isValid(List.of(
			attribute("owner", "team-a"),
			attribute("Owner", "team-b")), null)).isTrue();
	}

	@Test
	void nullEntriesAndNullKeysAreIgnored() {
		assertThat(validator.isValid(new ArrayList<>(Arrays.asList(
			null,
			attribute(null, "a"),
			attribute(null, "b"),
			attribute("owner", "team-a"))), null)).isTrue();
	}

	@Test
	void nullList() {
		assertThat(validator.isValid(null, null)).isTrue();
	}

	@Test
	void withEmptyList() {
		assertThat(validator.isValid(emptyList(), null)).isTrue();
	}

	private static LabelAttribute attribute(final String key, final String value) {
		return LabelAttribute.create().withKey(key).withValue(value);
	}
}
