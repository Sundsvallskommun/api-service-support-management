package se.sundsvall.supportmanagement.api.validation.impl;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.LabelAttribute;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ValidLabelAttributesConstraintValidatorTest {

	private final ValidLabelAttributesConstraintValidator validator = new ValidLabelAttributesConstraintValidator();

	@Test
	void labelWithUniqueAttributeKeys() {
		assertThat(validator.isValid(List.of(label("A",
			LabelAttribute.create().withKey("escalationEmail").withValue("a@example.com"),
			LabelAttribute.create().withKey("owner").withValue("team-a"))), null)).isTrue();
	}

	@Test
	void labelWithDuplicateAttributeKeys() {
		assertThat(validator.isValid(List.of(label("A",
			LabelAttribute.create().withKey("escalationEmail").withValue("a@example.com"),
			LabelAttribute.create().withKey("escalationEmail").withValue("b@example.com"))), null)).isFalse();
	}

	@Test
	void duplicateKeyOnNestedChild() {
		final var child = label("CHILD",
			LabelAttribute.create().withKey("k").withValue("v1"),
			LabelAttribute.create().withKey("k").withValue("v2"));
		final var parent = Label.create().withResourceName("PARENT").withLabels(List.of(child));

		assertThat(validator.isValid(List.of(parent), null)).isFalse();
	}

	@Test
	void siblingsCarryingTheSameKeyAreAllowed() {
		assertThat(validator.isValid(List.of(
			label("A", LabelAttribute.create().withKey("escalationEmail").withValue("a@example.com")),
			label("B", LabelAttribute.create().withKey("escalationEmail").withValue("b@example.com"))), null)).isTrue();
	}

	@Test
	void nullList() {
		assertThat(validator.isValid(null, null)).isTrue();
	}

	@Test
	void withEmptyList() {
		assertThat(validator.isValid(emptyList(), null)).isTrue();
	}

	@Test
	void labelWithoutAttributes() {
		assertThat(validator.isValid(List.of(Label.create().withResourceName("A")), null)).isTrue();
	}

	private static Label label(String resourceName, LabelAttribute... attributes) {
		return Label.create().withResourceName(resourceName).withAttributes(List.of(attributes));
	}
}
