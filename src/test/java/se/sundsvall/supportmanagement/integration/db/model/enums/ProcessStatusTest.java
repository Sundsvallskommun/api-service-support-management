package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RETRYING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.values;

/**
 * Every value is listed, and the listing is checked against the enum, so a value added later fails here rather than
 * silently defaulting to something. Which value is terminal decides whether an errand can be given a second live
 * process instance, and that is not a judgement to leave to whoever adds the next state.
 */
class ProcessStatusTest {

	private static Stream<Arguments> terminality() {
		return Stream.of(
			arguments(RUNNING, false),
			arguments(WAITING, false),
			arguments(RETRYING, false),
			arguments(COMPLETED, true),
			arguments(FAILED, true));
	}

	@Test
	void enums() {
		assertThat(values()).containsExactlyInAnyOrder(RUNNING, WAITING, RETRYING, COMPLETED, FAILED);
	}

	@ParameterizedTest
	@MethodSource("terminality")
	void isTerminal(final ProcessStatus status, final boolean expected) {
		assertThat(status.isTerminal()).isEqualTo(expected);
	}

	@Test
	@DisplayName("Verification that every value of the enum is covered by the table above")
	void everyValueIsAccountedFor() {
		assertThat(terminality().map(arguments -> arguments.get()[0]))
			.containsExactlyInAnyOrder((Object[]) values());
	}

	@Test
	@DisplayName("Verification that a process which is merely waiting is not read as one that has run its course")
	void waitingIsNotTerminal() {
		assertThat(WAITING.isTerminal()).isFalse();
	}
}
