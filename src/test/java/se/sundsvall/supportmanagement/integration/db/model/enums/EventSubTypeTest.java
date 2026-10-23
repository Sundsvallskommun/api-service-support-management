package se.sundsvall.supportmanagement.integration.db.model.enums;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ASSIGNMENT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ATTACHMENT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.DECISION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND_ACTIVATION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.HANDOVER_IN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.HANDOVER_OUT;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.LABEL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE_CONVERSATION_EXTERNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE_CONVERSATION_INTERNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE_EMAIL_EXTERNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE_SMS_EXTERNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.MESSAGE_WEB_MESSAGE_EXTERNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.NOTE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.PHASE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.PROCESS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SIGNAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.STATUS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SUSPENSION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.SYSTEM;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.values;

class EventSubTypeTest {

	private static Stream<Arguments> commands() {
		return Stream.of(
			arguments(ASSIGNMENT, false),
			arguments(ATTACHMENT, false),
			arguments(DECISION, false),
			arguments(ERRAND, false),
			arguments(ERRAND_ACTIVATION, false),
			arguments(HANDOVER_IN, false),
			arguments(HANDOVER_OUT, false),
			arguments(LABEL, false),
			arguments(MESSAGE_CONVERSATION_EXTERNAL, false),
			arguments(MESSAGE_CONVERSATION_INTERNAL, false),
			arguments(MESSAGE_EMAIL_EXTERNAL, false),
			arguments(MESSAGE_SMS_EXTERNAL, false),
			arguments(MESSAGE_WEB_MESSAGE_EXTERNAL, false),
			arguments(NOTE, false),
			arguments(PHASE, false),
			arguments(PROCESS, true),
			arguments(SIGNAL, true),
			arguments(STATUS, false),
			arguments(SYSTEM, false),
			arguments(SUSPENSION, false));
	}

	@Test
	void enums() {
		assertThat(values()).containsExactlyInAnyOrder(ASSIGNMENT, ATTACHMENT, DECISION, ERRAND, ERRAND_ACTIVATION, HANDOVER_IN, HANDOVER_OUT, LABEL, MESSAGE_CONVERSATION_EXTERNAL, MESSAGE_CONVERSATION_INTERNAL, MESSAGE_EMAIL_EXTERNAL,
			MESSAGE_SMS_EXTERNAL, MESSAGE_WEB_MESSAGE_EXTERNAL, NOTE, PHASE, PROCESS,
			SIGNAL, STATUS, SYSTEM, SUSPENSION);
	}

	@Test
	void enumValues() {
		assertThat(ATTACHMENT.getValue()).isEqualTo("ATTACHMENT");
		assertThat(DECISION.getValue()).isEqualTo("DECISION");
		assertThat(ERRAND.getValue()).isEqualTo("ERRAND");
		assertThat(ERRAND_ACTIVATION.getValue()).isEqualTo("ERRAND_ACTIVATION");
		assertThat(LABEL.getValue()).isEqualTo("LABEL");
		assertThat(MESSAGE_CONVERSATION_EXTERNAL.getValue()).isEqualTo("MESSAGE_CONVERSATION_EXTERNAL");
		assertThat(MESSAGE_CONVERSATION_INTERNAL.getValue()).isEqualTo("MESSAGE_CONVERSATION_INTERNAL");
		assertThat(NOTE.getValue()).isEqualTo("NOTE");
		assertThat(PHASE.getValue()).isEqualTo("PHASE");
		assertThat(PROCESS.getValue()).isEqualTo("PROCESS");
		assertThat(SIGNAL.getValue()).isEqualTo("SIGNAL");
		assertThat(STATUS.getValue()).isEqualTo("STATUS");
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

	@ParameterizedTest
	@CsvSource(nullValues = "null", value = {
		// A filter on a subtype selects that subtype
		"ATTACHMENT, ATTACHMENT, true",
		// and every subtype refining it
		"ERRAND, STATUS, true",
		"ERRAND, PHASE, true",
		// while a filter on a refined subtype selects only that one
		"MESSAGE_CONVERSATION_INTERNAL, MESSAGE_CONVERSATION_INTERNAL, true",
		"MESSAGE_CONVERSATION_INTERNAL, MESSAGE_CONVERSATION_EXTERNAL, false",
		"MESSAGE_CONVERSATION_EXTERNAL, MESSAGE_CONVERSATION_INTERNAL, false",
		"STATUS, STATUS, true",
		"STATUS, ERRAND, false",
		"STATUS, PHASE, false",
		"PHASE, ERRAND, false",
		"PHASE, STATUS, false",
		// A label being added is no change to the errand to a filter on ERRAND, nor the other way around
		"LABEL, LABEL, true",
		"ERRAND, LABEL, false",
		// Nor is a draft being made active, which is told alongside the update that made it active
		"ERRAND_ACTIVATION, ERRAND_ACTIVATION, true",
		"ERRAND, ERRAND_ACTIVATION, false",
		"ERRAND_ACTIVATION, ERRAND, false",
		"LABEL, ERRAND, false",
		// Unrelated subtypes do not select one another
		"ATTACHMENT, MESSAGE_CONVERSATION_EXTERNAL, false",
		"HANDOVER_IN, HANDOVER_OUT, false",
		// A subtype not known here is selected by itself alone
		"CUSTOM, CUSTOM, true",
		"ERRAND, CUSTOM, false",
		"ERRAND, null, false"
	})
	void selects(final String filterSubtype, final String eventSubtype, final boolean expected) {
		assertThat(EventSubType.selects(filterSubtype, eventSubtype)).isEqualTo(expected);
	}
}
