package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.LabelAttribute;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ValidProcessLabelAttributesConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext contextMock;

	@Mock
	private ConstraintViolationBuilder builderMock;

	private final ValidProcessLabelAttributesConstraintValidator validator = new ValidProcessLabelAttributesConstraintValidator();

	private final List<String> messages = new ArrayList<>();

	@BeforeEach
	void setUp() {
		lenient().when(contextMock.buildConstraintViolationWithTemplate(anyString())).thenAnswer(invocation -> {
			messages.add(invocation.getArgument(0));
			return builderMock;
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"AUTOMATIC", "MANUAL"
	})
	@DisplayName("Verification that a label carrying a process key and either start mode is valid")
	void aLabelWithAKeyAndAStartModeIsValid(final String startMode) {
		assertValid(List.of(label("TILLSYN", attribute("processKey", "alkt-tillsyn"), attribute("processStartMode", startMode))));
	}

	@Test
	@DisplayName("Verification that labels carrying neither attribute, or only the key, are valid")
	void labelsWithoutAStartModeAreValid() {
		assertValid(List.of(
			label("ANSOKAN", attribute("processKey", "alkt-ansokan")),
			label("BRADSKANDE", attribute("escalationEmail", "a@example.com")),
			Label.create().withResourceName("TOM")));
		assertValid(null);
		assertValid(emptyList());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"manual", "Manual", " MANUAL", "MANUAL ", "SEMI", "AUTO"
	})
	@DisplayName("Verification that a start mode other than exactly AUTOMATIC or MANUAL is invalid")
	void aStartModeThatIsNotExactlyOneOfTheModesIsInvalid(final String startMode) {
		assertInvalid(List.of(label("TILLSYN", attribute("processKey", "alkt-tillsyn"), attribute("processStartMode", startMode))),
			"label 'TILLSYN' has the processStartMode '" + startMode + "', which must be exactly one of [AUTOMATIC, MANUAL]");
	}

	@Test
	@DisplayName("Verification that a start mode on a label without a process key is invalid")
	void aStartModeWithoutAKeyIsInvalid() {
		assertInvalid(List.of(label("TILLSYN", attribute("processStartMode", "MANUAL"))),
			"label 'TILLSYN' has a processStartMode but no processKey, and a start mode means nothing without the process it starts");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"processstartmode", "PROCESSSTARTMODE", "ProcessStartMode", " processStartMode", "processStartMode "
	})
	@DisplayName("Verification that a key spelled like processStartMode in another way is invalid")
	void aMisspelledStartModeKeyIsInvalid(final String key) {
		assertInvalid(List.of(label("TILLSYN", attribute("processKey", "alkt-tillsyn"), attribute(key, "MANUAL"))),
			"label 'TILLSYN' has the attribute '" + key + "', which is read only when spelled exactly 'processStartMode'");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"processkey", "PROCESSKEY", "ProcessKey", "processKey "
	})
	@DisplayName("Verification that a key spelled like processKey in another way is invalid")
	void aMisspelledProcessKeyIsInvalid(final String key) {
		assertInvalid(List.of(label("TILLSYN", attribute(key, "alkt-tillsyn"))),
			"label 'TILLSYN' has the attribute '" + key + "', which is read only when spelled exactly 'processKey'");
	}

	@Test
	@DisplayName("Verification that every fault in a tree is reported on its own, naming the label by its path of resource names")
	void everyFaultInATreeIsReportedNamingItsLabel() {
		final var child = label("TILLSYN", attribute("processStartMode", "manual"));
		final var parent = label("ALKT", attribute("processkey", "alkt-ansokan")).withLabels(List.of(child));

		assertInvalid(List.of(parent),
			"label 'ALKT' has the attribute 'processkey', which is read only when spelled exactly 'processKey'",
			"label 'ALKT/TILLSYN' has the processStartMode 'manual', which must be exactly one of [AUTOMATIC, MANUAL]",
			"label 'ALKT/TILLSYN' has a processStartMode but no processKey, and a start mode means nothing without the process it starts");
	}

	@Test
	@DisplayName("Verification that a process key as long as a process key may be is valid, also with blanks around it")
	void aProcessKeyOfTheLongestLengthIsValid() {
		assertValid(List.of(label("TILLSYN", attribute("processKey", " " + "k".repeat(128) + " "))));
	}

	@Test
	@DisplayName("Verification that a process key one character longer than a process key may be is invalid")
	void aProcessKeyTooLongIsInvalid() {
		assertInvalid(List.of(label("ALKT", attribute("processKey", "alkt-ansokan")).withLabels(List.of(label("TILLSYN", attribute("processKey", "k".repeat(129)))))),
			"label 'ALKT/TILLSYN' has a processKey of 129 characters, and a process key may hold at most 128");
	}

	@Test
	@DisplayName("Verification that a value too long to repeat is cut in the message")
	void anOversizedValueIsCutInTheMessage() {
		final var oversized = "M".repeat(200);

		assertThat(validator.isValid(List.of(label("TILLSYN", attribute("processKey", "alkt-tillsyn"), attribute("processStartMode", oversized))), contextMock)).isFalse();

		assertThat(messages).singleElement().satisfies(message -> assertThat(message).doesNotContain(oversized).contains("M".repeat(61) + "..."));
	}

	@Test
	@DisplayName("Verification that braces in a value are escaped, so that the message is not interpolated")
	void aValueIsEscapedInTheMessage() {
		assertThat(validator.isValid(List.of(label("TILLSYN", attribute("processKey", "alkt-tillsyn"), attribute("processStartMode", "${1+1}{x}"))), contextMock)).isFalse();

		assertThat(messages).singleElement().satisfies(message -> assertThat(message).contains("\\$\\{1+1\\}\\{x\\}"));
	}

	private void assertValid(final List<Label> labels) {
		assertThat(validator.isValid(labels, contextMock)).isTrue();
		verifyNoInteractions(contextMock);
	}

	private void assertInvalid(final List<Label> labels, final String... expectedMessages) {
		assertThat(validator.isValid(labels, contextMock)).isFalse();

		verify(contextMock).disableDefaultConstraintViolation();
		assertThat(messages).containsExactly(expectedMessages);
	}

	private static Label label(final String resourceName, final LabelAttribute... attributes) {
		return Label.create().withResourceName(resourceName).withAttributes(new ArrayList<>(Arrays.asList(attributes)));
	}

	private static LabelAttribute attribute(final String key, final String value) {
		return LabelAttribute.create().withKey(key).withValue(value);
	}
}
