package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ATTACHMENT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.DECISION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.HANDOVER_IN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.HANDOVER_OUT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.NOTE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.PROCESS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SIGNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SUSPENSION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SYSTEM;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.values;

class EventSubTypeTest {

	private static Stream<Arguments> commands() {
		return Stream.of(
			arguments(ATTACHMENT, false),
			arguments(DECISION, false),
			arguments(ERRAND, false),
			arguments(HANDOVER_IN, false),
			arguments(HANDOVER_OUT, false),
			arguments(MESSAGE, false),
			arguments(NOTE, false),
			arguments(PROCESS, true),
			arguments(SIGNAL, true),
			arguments(SYSTEM, false),
			arguments(SUSPENSION, false));
	}

	@Test
	void enums() {
		assertThat(values()).containsExactlyInAnyOrder(ATTACHMENT, DECISION, ERRAND, HANDOVER_IN, HANDOVER_OUT, MESSAGE, NOTE, PROCESS, SIGNAL, SYSTEM, SUSPENSION);
	}

	@Test
	void enumValues() {
		assertThat(ATTACHMENT.getValue()).isEqualTo("ATTACHMENT");
		assertThat(DECISION.getValue()).isEqualTo("DECISION");
		assertThat(ERRAND.getValue()).isEqualTo("ERRAND");
		assertThat(MESSAGE.getValue()).isEqualTo("MESSAGE");
		assertThat(NOTE.getValue()).isEqualTo("NOTE");
		assertThat(PROCESS.getValue()).isEqualTo("PROCESS");
		assertThat(SIGNAL.getValue()).isEqualTo("SIGNAL");
		assertThat(SYSTEM.getValue()).isEqualTo("SYSTEM");
		assertThat(SUSPENSION.getValue()).isEqualTo("SUSPENSION");
	}

	@ParameterizedTest
	@MethodSource("commands")
	void isCommand(final EventSubType subType, final boolean expected) {
		assertThat(subType.isCommand()).isEqualTo(expected);
	}

	@Test
	@DisplayName("Verification that every value of the enum is covered by the table above, since a command passes the loop guard")
	void everyValueIsAccountedFor() {
		assertThat(commands().map(arguments -> arguments.get()[0]))
			.containsExactlyInAnyOrder((Object[]) values());
	}
}
