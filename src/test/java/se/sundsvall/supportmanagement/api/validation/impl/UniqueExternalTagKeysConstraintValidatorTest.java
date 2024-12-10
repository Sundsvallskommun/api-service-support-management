package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.errand.ExternalTag.create;

import java.util.List;
import org.junit.jupiter.api.Test;

class UniqueExternalTagKeysConstraintValidatorTest {

	private UniqueExternalTagKeysConstraintValidator validator = new UniqueExternalTagKeysConstraintValidator();

	@Test
	void withNonUniqueKeys() {
		assertThat(validator.isValid(List.of(
			create().withKey("key1").withValue("value1"),
			create().withKey("key1").withValue("value2")), null)).isFalse();
	}

	@Test
	void withUniqueKeys() {
		assertThat(validator.isValid(List.of(
			create().withKey("key1").withValue("value1"),
			create().withKey("key2").withValue("value1")), null)).isTrue();
	}

	@Test
	void WithNullAsList() {
		assertThat(validator.isValid(null, null)).isTrue();
	}

	@Test
	void withEmptyList() {
		assertThat(validator.isValid(emptyList(), null)).isTrue();
	}
}
