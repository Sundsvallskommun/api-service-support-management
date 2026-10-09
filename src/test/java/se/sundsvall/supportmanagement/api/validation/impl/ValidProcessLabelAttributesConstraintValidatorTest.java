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
	@DisplayName("Verification that a process key with either start mode is valid")
	void aKeyWithAStartModeIsValid(final String startMode) {
		assertValid(attributes(attribute("processKey", "alkt-tillsyn"), attribute("processStartMode", startMode)));
	}

	@Test
	@DisplayName("Verification that attributes carrying no start mode, or none at all, are valid")
	void attributesWithoutAStartModeAreValid() {
		assertValid(attributes(attribute("processKey", "alkt-ansokan")));
		assertValid(attributes(attribute("escalationEmail", "a@example.com")));
		assertValid(attributes(null, attribute(null, "value")));
		assertValid(null);
		assertValid(emptyList());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"manual", "Manual", " MANUAL", "MANUAL ", "SEMI", "AUTO"
	})
	@DisplayName("Verification that a start mode other than exactly AUTOMATIC or MANUAL is invalid")
	void aStartModeThatIsNotExactlyOneOfTheModesIsInvalid(final String startMode) {
		assertInvalid(attributes(attribute("processKey", "alkt-tillsyn"), attribute("processStartMode", startMode)),
			"the processStartMode '" + startMode + "' must be exactly one of [AUTOMATIC, MANUAL]");
	}

	@Test
	@DisplayName("Verification that a start mode without a process key is invalid")
	void aStartModeWithoutAKeyIsInvalid() {
		assertInvalid(attributes(attribute("processStartMode", "MANUAL")),
			"a processStartMode needs a processKey on the same label, since a start mode means nothing without the process it starts");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"processstartmode", "PROCESSSTARTMODE", "ProcessStartMode", " processStartMode", "processStartMode "
	})
	@DisplayName("Verification that a key spelled like processStartMode in another way is invalid")
	void aMisspelledStartModeKeyIsInvalid(final String key) {
		assertInvalid(attributes(attribute("processKey", "alkt-tillsyn"), attribute(key, "MANUAL")),
			"the attribute '" + key + "' is read only when spelled exactly 'processStartMode'");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"processkey", "PROCESSKEY", "ProcessKey", "processKey "
	})
	@DisplayName("Verification that a key spelled like processKey in another way is invalid")
	void aMisspelledProcessKeyIsInvalid(final String key) {
		assertInvalid(attributes(attribute(key, "alkt-tillsyn")),
			"the attribute '" + key + "' is read only when spelled exactly 'processKey'");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"true", "false"
	})
	@DisplayName("Verification that processBlocked set to exactly true or false is valid, with or without a process key")
	void aBlockedOfExactlyTrueOrFalseIsValid(final String blocked) {
		assertValid(attributes(attribute("processBlocked", blocked)));
		assertValid(attributes(attribute("processKey", "alkt-tillsyn"), attribute("processBlocked", blocked)));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"TRUE", "True", " true", "false ", "yes", "1", "blocked"
	})
	@DisplayName("Verification that processBlocked other than exactly true or false is invalid")
	void aBlockedThatIsNotExactlyTrueOrFalseIsInvalid(final String blocked) {
		assertInvalid(attributes(attribute("processBlocked", blocked)),
			"the processBlocked '" + blocked + "' must be exactly one of [true, false]");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"processblocked", "PROCESSBLOCKED", "ProcessBlocked", " processBlocked", "processBlocked "
	})
	@DisplayName("Verification that a key spelled like processBlocked in another way is invalid")
	void aMisspelledBlockedKeyIsInvalid(final String key) {
		assertInvalid(attributes(attribute(key, "true")),
			"the attribute '" + key + "' is read only when spelled exactly 'processBlocked'");
	}

	@Test
	@DisplayName("Verification that every fault among the attributes is reported on its own")
	void everyFaultIsReportedOnItsOwn() {
		assertInvalid(attributes(attribute("processkey", "alkt-ansokan"), attribute("processStartMode", "manual")),
			"the attribute 'processkey' is read only when spelled exactly 'processKey'",
			"the processStartMode 'manual' must be exactly one of [AUTOMATIC, MANUAL]",
			"a processStartMode needs a processKey on the same label, since a start mode means nothing without the process it starts");
	}

	@Test
	@DisplayName("Verification that a process key as long as a process key may be is valid, also with blanks around it")
	void aProcessKeyOfTheLongestLengthIsValid() {
		assertValid(attributes(attribute("processKey", " " + "k".repeat(128) + " ")));
	}

	@Test
	@DisplayName("Verification that a process key one character longer than a process key may be is invalid")
	void aProcessKeyTooLongIsInvalid() {
		assertInvalid(attributes(attribute("processKey", "k".repeat(129))),
			"the processKey has 129 characters, and a process key may hold at most 128");
	}

	@Test
	@DisplayName("Verification that a value too long to repeat is cut in the message")
	void anOversizedValueIsCutInTheMessage() {
		final var oversized = "M".repeat(200);

		assertThat(validator.isValid(attributes(attribute("processKey", "alkt-tillsyn"), attribute("processStartMode", oversized)), contextMock)).isFalse();

		assertThat(messages).singleElement().satisfies(message -> assertThat(message).doesNotContain(oversized).contains("M".repeat(61) + "..."));
	}

	@Test
	@DisplayName("Verification that braces in a value are escaped, so that the message is not interpolated")
	void aValueIsEscapedInTheMessage() {
		assertThat(validator.isValid(attributes(attribute("processKey", "alkt-tillsyn"), attribute("processStartMode", "${1+1}{x}")), contextMock)).isFalse();

		assertThat(messages).singleElement().satisfies(message -> assertThat(message).contains("\\$\\{1+1\\}\\{x\\}"));
	}

	private void assertValid(final List<LabelAttribute> attributes) {
		assertThat(validator.isValid(attributes, contextMock)).isTrue();
		verifyNoInteractions(contextMock);
	}

	private void assertInvalid(final List<LabelAttribute> attributes, final String... expectedMessages) {
		assertThat(validator.isValid(attributes, contextMock)).isFalse();

		verify(contextMock).disableDefaultConstraintViolation();
		assertThat(messages).containsExactly(expectedMessages);
	}

	private static List<LabelAttribute> attributes(final LabelAttribute... attributes) {
		return new ArrayList<>(Arrays.asList(attributes));
	}

	private static LabelAttribute attribute(final String key, final String value) {
		return LabelAttribute.create().withKey(key).withValue(value);
	}
}
