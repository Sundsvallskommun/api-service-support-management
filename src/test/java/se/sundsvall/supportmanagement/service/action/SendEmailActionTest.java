package se.sundsvall.supportmanagement.service.action;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.config.action.enums.OperationType;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Labels;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendEmailActionTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "testNamespace";
	private static final String LABEL_ID_1 = "label-id-1";
	private static final String LABEL_ID_2 = "label-id-2";
	private static final String STATUS_OPEN = "OPEN";
	private static final String STATUS_CLOSED = "CLOSED";
	private static final String ERRAND_ID = "errand-id";
	private static final String ERRAND_NUMBER = "errand-number";
	private static final String RECIPIENT_ADDRESS = "recipient@test.com";
	private static final String SENDER_ADDRESS = "sender@test.com";
	private static final String EMAIL_SUBJECT = "Test subject";
	private static final String EMAIL_BODY = "Test body";
	private static final String ERRAND_BASE_URL = "https://support.sundsvall.se";
	private static final Instant FIXED_INSTANT = Instant.parse("2026-06-05T10:00:00Z");
	private static final ZoneId ZONE_ID = ZoneId.of("UTC");

	@Mock
	private MetadataService metadataService;

	@Mock
	private CommunicationService communicationService;

	@Mock
	private CommunicationRepository communicationRepository;

	@Mock
	private Clock clock;

	@InjectMocks
	private SendEmailAction sendEmailAction;

	@Captor
	private ArgumentCaptor<EmailRequest> emailRequestCaptor;

	@Test
	void getName() {
		assertThat(sendEmailAction.getName()).isEqualTo("SEND_EMAIL");
	}

	@Test
	void getDescription() {
		assertThat(sendEmailAction.getDescription()).isEqualTo("Sends an email when conditions are met");
	}

	@Test
	void getConditionDefinitions() {
		when(metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID, Sort.unsorted())).thenReturn(List.of(
			Status.create().withName(STATUS_OPEN),
			Status.create().withName(STATUS_CLOSED)));
		when(metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Labels.create()
			.withLabelStructure(List.of(
				Label.create().withId(LABEL_ID_1).withDisplayName("Label 1").withClassification("type").withResourceName("LABEL_1"),
				Label.create().withId(LABEL_ID_2).withDisplayName("Label 2").withClassification("type").withResourceName("LABEL_2"))));

		var result = sendEmailAction.getConditionDefinitions(MUNICIPALITY_ID, NAMESPACE);

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getKey()).isEqualTo("status");
		assertThat(result.getFirst().getMandatory()).isFalse();
		assertThat(result.getFirst().getPossibleValues()).hasSize(2);
		assertThat(result.get(1).getKey()).isEqualTo("hasLabel");
		assertThat(result.get(1).getMandatory()).isFalse();
		assertThat(result.get(1).getPossibleValues()).hasSize(2);

		verify(metadataService).findStatuses(NAMESPACE, MUNICIPALITY_ID, Sort.unsorted());
		verify(metadataService).findLabels(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(metadataService);
	}

	@Test
	void getParameterDefinitions() {
		var result = sendEmailAction.getParameterDefinitions(MUNICIPALITY_ID, NAMESPACE);

		assertThat(result).hasSize(7);
		assertThat(result.getFirst().getKey()).isEqualTo("recipient");
		assertThat(result.getFirst().getMandatory()).isTrue();
		assertThat(result.get(1).getKey()).isEqualTo("sender");
		assertThat(result.get(1).getMandatory()).isTrue();
		assertThat(result.get(2).getKey()).isEqualTo("subject");
		assertThat(result.get(2).getMandatory()).isTrue();
		assertThat(result.get(3).getKey()).isEqualTo("body");
		assertThat(result.get(3).getMandatory()).isTrue();
		assertThat(result.get(4).getKey()).isEqualTo("addLinkToErrandInBody");
		assertThat(result.get(4).getMandatory()).isTrue();
		assertThat(result.get(4).getPossibleValues()).hasSize(2);
		assertThat(result.get(5).getKey()).isEqualTo("baseUrl");
		assertThat(result.get(5).getMandatory()).isFalse();
		assertThat(result.get(6).getKey()).isEqualTo("duration");
		assertThat(result.get(6).getMandatory()).isFalse();

		verifyNoMoreInteractions(metadataService);
	}

	// validateConditions tests
	@Test
	void validateConditionsWithValidStatusAndLabel() {
		when(metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID, Sort.unsorted())).thenReturn(List.of(Status.create().withName(STATUS_OPEN)));
		when(metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Labels.create()
			.withLabelStructure(List.of(Label.create().withId(LABEL_ID_1).withDisplayName("Label 1").withClassification("type").withResourceName("LABEL_1"))));

		sendEmailAction.validateConditions(MUNICIPALITY_ID, NAMESPACE, Map.of(
			"status", List.of(STATUS_OPEN),
			"hasLabel", List.of(LABEL_ID_1)));

		verify(metadataService).findStatuses(NAMESPACE, MUNICIPALITY_ID, Sort.unsorted());
		verify(metadataService).findLabels(NAMESPACE, MUNICIPALITY_ID);
		verifyNoMoreInteractions(metadataService);
	}

	@Test
	void validateConditionsWithInvalidStatus() {
		when(metadataService.findStatuses(NAMESPACE, MUNICIPALITY_ID, Sort.unsorted())).thenReturn(List.of(Status.create().withName(STATUS_OPEN)));

		final var conditions = Map.of("status", List.of("INVALID_STATUS"));
		assertThatThrownBy(() -> sendEmailAction.validateConditions(MUNICIPALITY_ID, NAMESPACE, conditions))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Status 'INVALID_STATUS' is not valid for this namespace");
	}

	@Test
	void validateConditionsWithInvalidLabel() {
		when(metadataService.findLabels(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Labels.create()
			.withLabelStructure(List.of(Label.create().withId(LABEL_ID_1).withDisplayName("Label 1").withClassification("type").withResourceName("LABEL_1"))));

		final var conditions = Map.of("hasLabel", List.of("invalid-label-id"));
		assertThatThrownBy(() -> sendEmailAction.validateConditions(MUNICIPALITY_ID, NAMESPACE, conditions))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Label ID 'invalid-label-id' is not valid");
	}

	@ParameterizedTest
	@MethodSource("invalidConditionsProvider")
	void validateConditionsWithInvalidInput(Map<String, List<String>> conditions, String expectedMessage) {
		assertThatThrownBy(() -> sendEmailAction.validateConditions(MUNICIPALITY_ID, NAMESPACE, conditions))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining(expectedMessage);
	}

	private static Stream<Arguments> invalidConditionsProvider() {
		return Stream.of(
			Arguments.of(
				Map.of("invalidKey", List.of("value")),
				"Key 'invalidKey' is not valid"),
			Arguments.of(
				Map.of("duration", List.of("PT1H")),
				"Key 'duration' is not valid"));
	}

	// validateParameters tests
	@Test
	void validateParametersWithValidDuration() {
		sendEmailAction.validateParameters(MUNICIPALITY_ID, NAMESPACE, createParameters(
			"recipient", "test@test.com",
			"sender", SENDER_ADDRESS,
			"subject", EMAIL_SUBJECT,
			"body", EMAIL_BODY,
			"addLinkToErrandInBody", "true",
			"baseUrl", ERRAND_BASE_URL,
			"duration", "PT1H"));
	}

	@Test
	void validateParametersWithAllValid() {
		sendEmailAction.validateParameters(MUNICIPALITY_ID, NAMESPACE, createParameters(
			"recipient", "test@test.com",
			"sender", SENDER_ADDRESS,
			"subject", EMAIL_SUBJECT,
			"body", EMAIL_BODY,
			"addLinkToErrandInBody", "true",
			"baseUrl", ERRAND_BASE_URL));
	}

	@ParameterizedTest
	@MethodSource("invalidParametersProvider")
	void validateParametersWithInvalidInput(Map<String, List<String>> parameters, String expectedMessage) {
		assertThatThrownBy(() -> sendEmailAction.validateParameters(MUNICIPALITY_ID, NAMESPACE, parameters))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining(expectedMessage);
	}

	private static Stream<Arguments> invalidParametersProvider() {
		return Stream.of(
			Arguments.of(
				createParameters("recipient", "test@test.com", "sender", SENDER_ADDRESS, "subject", EMAIL_SUBJECT, "body", EMAIL_BODY, "addLinkToErrandInBody", "true", "invalidKey", "value"),
				"Key 'invalidKey' is not valid"),
			Arguments.of(
				Map.of("sender", List.of(SENDER_ADDRESS), "subject", List.of(EMAIL_SUBJECT), "body", List.of(EMAIL_BODY), "addLinkToErrandInBody", List.of("true")),
				"Key 'recipient' is mandatory and cannot be empty"),
			Arguments.of(
				Map.of("recipient", List.of("test@test.com"), "subject", List.of(EMAIL_SUBJECT), "body", List.of(EMAIL_BODY), "addLinkToErrandInBody", List.of("true")),
				"Key 'sender' is mandatory and cannot be empty"),
			Arguments.of(
				Map.of("recipient", List.of("test@test.com"), "sender", List.of(SENDER_ADDRESS), "body", List.of(EMAIL_BODY), "addLinkToErrandInBody", List.of("true")),
				"Key 'subject' is mandatory and cannot be empty"),
			Arguments.of(
				Map.of("recipient", List.of("test@test.com"), "sender", List.of(SENDER_ADDRESS), "subject", List.of(EMAIL_SUBJECT), "addLinkToErrandInBody", List.of("true")),
				"Key 'body' is mandatory and cannot be empty"),
			Arguments.of(
				Map.of("recipient", List.of("test@test.com"), "sender", List.of(SENDER_ADDRESS), "subject", List.of(EMAIL_SUBJECT), "body", List.of(EMAIL_BODY)),
				"Key 'addLinkToErrandInBody' is mandatory and cannot be empty"),
			Arguments.of(
				Map.of("recipient", List.of("test1@test.com", "test2@test.com"), "sender", List.of(SENDER_ADDRESS), "subject", List.of(EMAIL_SUBJECT), "body", List.of(EMAIL_BODY), "addLinkToErrandInBody", List.of("true")),
				"Cannot handle multiple values of key 'recipient'"),
			Arguments.of(
				Map.of("recipient", List.of("test@test.com"), "sender", List.of(SENDER_ADDRESS), "subject", List.of(EMAIL_SUBJECT), "body", List.of(EMAIL_BODY), "addLinkToErrandInBody", List.of("notABoolean")),
				"Value 'notABoolean' is not valid for key 'addLinkToErrandInBody'"),
			Arguments.of(
				createParameters("recipient", "test@test.com", "sender", SENDER_ADDRESS, "subject", EMAIL_SUBJECT, "body", EMAIL_BODY, "addLinkToErrandInBody", "true"),
				"Key 'baseUrl' is required when 'addLinkToErrandInBody' is true"),
			Arguments.of(
				createParameters("recipient", "test@test.com", "sender", SENDER_ADDRESS, "subject", EMAIL_SUBJECT, "body", EMAIL_BODY, "addLinkToErrandInBody", "true", "baseUrl", ERRAND_BASE_URL, "duration", "not-a-duration"),
				"Could not parse duration 'not-a-duration'"));
	}

	private static Map<String, List<String>> createParameters(String... keyValues) {
		var map = new HashMap<String, List<String>>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put(keyValues[i], List.of(keyValues[i + 1]));
		}
		return map;
	}

	// actionFulfilled tests

	@Test
	void actionFulfilledWhenMatchingEmailExists() {
		var errand = ErrandEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withId(ERRAND_ID)
			.withErrandNumber(ERRAND_NUMBER);

		var communication = CommunicationEntity.create()
			.withType(CommunicationType.EMAIL)
			.withDirection(Direction.OUTBOUND)
			.withSender(SENDER_ADDRESS)
			.withRecipients(List.of(RECIPIENT_ADDRESS))
			.withSubject(EMAIL_SUBJECT + " - " + ERRAND_NUMBER);

		when(communicationRepository.findByErrandNumber(ERRAND_NUMBER)).thenReturn(List.of(communication));

		assertThat(sendEmailAction.actionFulfilled(errand, Map.of(
			"recipient", List.of(RECIPIENT_ADDRESS),
			"sender", List.of(SENDER_ADDRESS),
			"subject", List.of(EMAIL_SUBJECT),
			"body", List.of(EMAIL_BODY)))).isTrue();

		verify(communicationRepository).findByErrandNumber(ERRAND_NUMBER);
	}

	@Test
	void actionFulfilledWhenNoMatchingEmailExists() {
		var errand = ErrandEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withId(ERRAND_ID)
			.withErrandNumber(ERRAND_NUMBER);

		when(communicationRepository.findByErrandNumber(ERRAND_NUMBER)).thenReturn(List.of());

		assertThat(sendEmailAction.actionFulfilled(errand, Map.of(
			"recipient", List.of(RECIPIENT_ADDRESS),
			"sender", List.of(SENDER_ADDRESS),
			"subject", List.of(EMAIL_SUBJECT),
			"body", List.of(EMAIL_BODY)))).isFalse();

		verify(communicationRepository).findByErrandNumber(ERRAND_NUMBER);
	}

	@Test
	void actionFulfilledWhenEmailExistsButDifferentSender() {
		var errand = ErrandEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withId(ERRAND_ID)
			.withErrandNumber(ERRAND_NUMBER);

		var communication = CommunicationEntity.create()
			.withType(CommunicationType.EMAIL)
			.withDirection(Direction.OUTBOUND)
			.withSender("other@test.com")
			.withRecipients(List.of(RECIPIENT_ADDRESS))
			.withSubject(EMAIL_SUBJECT + " - " + ERRAND_NUMBER);

		when(communicationRepository.findByErrandNumber(ERRAND_NUMBER)).thenReturn(List.of(communication));

		assertThat(sendEmailAction.actionFulfilled(errand, Map.of(
			"recipient", List.of(RECIPIENT_ADDRESS),
			"sender", List.of(SENDER_ADDRESS),
			"subject", List.of(EMAIL_SUBJECT),
			"body", List.of(EMAIL_BODY)))).isFalse();

		verify(communicationRepository).findByErrandNumber(ERRAND_NUMBER);
	}

	// createAction tests
	@Test
	void createActionWhenConditionsMet() {
		when(clock.instant()).thenReturn(FIXED_INSTANT);
		when(clock.getZone()).thenReturn(ZONE_ID);

		var errand = ErrandEntity.create()
			.withStatus(STATUS_OPEN)
			.withLabels(List.of(ErrandLabelEmbeddable.create().withMetadataLabelId(LABEL_ID_1)));

		var config = ActionConfigEntity.create()
			.withActive(true);
		config.setConditions(new ArrayList<>(List.of(
			ActionConfigConditionEntity.create().withKey("status").withValues(List.of(STATUS_OPEN)),
			ActionConfigConditionEntity.create().withKey("hasLabel").withValues(List.of(LABEL_ID_1)))));
		config.setParameters(new ArrayList<>());

		var result = sendEmailAction.createAction(errand, config);

		assertThat(result).isPresent();
		assertThat(result.get().getActionConfigEntity()).isEqualTo(config);
		assertThat(result.get().getErrandEntity()).isEqualTo(errand);
		assertThat(result.get().getExecuteAfter()).isEqualTo(OffsetDateTime.ofInstant(FIXED_INSTANT, ZONE_ID));
	}

	@Test
	void createActionWithDuration() {
		when(clock.instant()).thenReturn(FIXED_INSTANT);
		when(clock.getZone()).thenReturn(ZONE_ID);

		var errand = ErrandEntity.create();

		var config = ActionConfigEntity.create()
			.withActive(true);
		config.setConditions(new ArrayList<>());
		config.setParameters(new ArrayList<>(List.of(
			ActionConfigParameterEntity.create().withKey("duration").withValues(List.of("PT2H")))));

		var result = sendEmailAction.createAction(errand, config);

		assertThat(result).isPresent();
		assertThat(result.get().getExecuteAfter()).isEqualTo(OffsetDateTime.ofInstant(FIXED_INSTANT, ZONE_ID).plusHours(2));
	}

	@Test
	void createActionWithoutDuration() {
		when(clock.instant()).thenReturn(FIXED_INSTANT);
		when(clock.getZone()).thenReturn(ZONE_ID);

		var errand = ErrandEntity.create();
		var config = ActionConfigEntity.create()
			.withActive(true);
		config.setConditions(new ArrayList<>());
		config.setParameters(new ArrayList<>());

		var result = sendEmailAction.createAction(errand, config);

		assertThat(result).isPresent();
		assertThat(result.get().getExecuteAfter()).isEqualTo(OffsetDateTime.ofInstant(FIXED_INSTANT, ZONE_ID));
	}

	@Test
	void createActionWhenStatusConditionNotMet() {
		var errand = ErrandEntity.create()
			.withStatus(STATUS_CLOSED)
			.withLabels(List.of());

		var config = ActionConfigEntity.create()
			.withActive(true);
		config.setConditions(new ArrayList<>(List.of(
			ActionConfigConditionEntity.create().withKey("status").withValues(List.of(STATUS_OPEN)))));
		config.setParameters(new ArrayList<>());

		var result = sendEmailAction.createAction(errand, config);

		assertThat(result).isEmpty();
	}

	@Test
	void createActionWhenHasLabelConditionNotMet() {
		var errand = ErrandEntity.create()
			.withStatus(STATUS_OPEN)
			.withLabels(List.of());

		var config = ActionConfigEntity.create()
			.withActive(true);
		config.setConditions(new ArrayList<>(List.of(
			ActionConfigConditionEntity.create().withKey("hasLabel").withValues(List.of(LABEL_ID_1)))));
		config.setParameters(new ArrayList<>());

		var result = sendEmailAction.createAction(errand, config);

		assertThat(result).isEmpty();
	}

	@Test
	void createActionWhenNotActive() {
		var errand = ErrandEntity.create()
			.withStatus(STATUS_OPEN)
			.withLabels(List.of());

		var config = ActionConfigEntity.create()
			.withActive(false);
		config.setConditions(new ArrayList<>());
		config.setParameters(new ArrayList<>());

		var result = sendEmailAction.createAction(errand, config);

		assertThat(result).isEmpty();
	}

	// executeAction tests

	@Test
	void executeAction() {
		var errand = ErrandEntity.create()
			.withErrandNumber(ERRAND_NUMBER);

		var config = ActionConfigEntity.create();
		config.setConditions(new ArrayList<>());
		config.setParameters(new ArrayList<>(List.of(
			ActionConfigParameterEntity.create().withKey("recipient").withValues(List.of(RECIPIENT_ADDRESS)),
			ActionConfigParameterEntity.create().withKey("sender").withValues(List.of(SENDER_ADDRESS)),
			ActionConfigParameterEntity.create().withKey("subject").withValues(List.of(EMAIL_SUBJECT)),
			ActionConfigParameterEntity.create().withKey("body").withValues(List.of(EMAIL_BODY)),
			ActionConfigParameterEntity.create().withKey("addLinkToErrandInBody").withValues(List.of("false")))));

		sendEmailAction.executeAction(errand, config);

		verify(communicationService).sendEmail(eq(errand), emailRequestCaptor.capture());
		var capturedRequest = emailRequestCaptor.getValue();
		assertThat(capturedRequest.getRecipient()).isEqualTo(RECIPIENT_ADDRESS);
		assertThat(capturedRequest.getSender()).isEqualTo(SENDER_ADDRESS);
		assertThat(capturedRequest.getSubject()).isEqualTo(EMAIL_SUBJECT + " - " + ERRAND_NUMBER);
		assertThat(capturedRequest.getMessage()).isNull();
		assertThat(capturedRequest.getHtmlMessage()).isEqualTo(EMAIL_BODY
			+ "<br><br><em>Detta är ett automatiskt meddelande. Svara inte på detta e-postmeddelande</em>");
	}

	@Test
	void executeActionWithAddLinkToErrandInBody() {
		var errand = ErrandEntity.create()
			.withId(ERRAND_ID)
			.withErrandNumber(ERRAND_NUMBER)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withStatus(STATUS_OPEN)
			.withLabels(List.of());

		var config = ActionConfigEntity.create()
			.withActive(true);
		config.setConditions(new ArrayList<>());
		config.setParameters(new ArrayList<>(List.of(
			ActionConfigParameterEntity.create().withKey("recipient").withValues(List.of(RECIPIENT_ADDRESS)),
			ActionConfigParameterEntity.create().withKey("sender").withValues(List.of(SENDER_ADDRESS)),
			ActionConfigParameterEntity.create().withKey("subject").withValues(List.of(EMAIL_SUBJECT)),
			ActionConfigParameterEntity.create().withKey("body").withValues(List.of(EMAIL_BODY)),
			ActionConfigParameterEntity.create().withKey("addLinkToErrandInBody").withValues(List.of("true")),
			ActionConfigParameterEntity.create().withKey("baseUrl").withValues(List.of(ERRAND_BASE_URL)))));

		sendEmailAction.executeAction(errand, config);

		verify(communicationService).sendEmail(eq(errand), emailRequestCaptor.capture());
		var capturedRequest = emailRequestCaptor.getValue();
		assertThat(capturedRequest.getRecipient()).isEqualTo(RECIPIENT_ADDRESS);
		assertThat(capturedRequest.getSender()).isEqualTo(SENDER_ADDRESS);
		assertThat(capturedRequest.getSubject()).isEqualTo(EMAIL_SUBJECT + " - " + ERRAND_NUMBER);
		assertThat(capturedRequest.getMessage()).isNull();
		assertThat(capturedRequest.getHtmlMessage()).isEqualTo(EMAIL_BODY + "<br><br><a href=\"" + ERRAND_BASE_URL + "/arende/" + ERRAND_NUMBER + "\">Öppna ärendet direkt i Draken</a>"
			+ "<br><br><em>Detta är ett automatiskt meddelande. Svara inte på detta e-postmeddelande</em>");
	}

	// conditionsFulfilled tests

	@Test
	void conditionsFulfilledWhenStatusMatches() {
		var errand = ErrandEntity.create().withStatus(STATUS_OPEN).withLabels(List.of());
		var config = ActionConfigEntity.create();
		config.setConditions(new ArrayList<>(List.of(
			ActionConfigConditionEntity.create().withKey("status").withValues(List.of(STATUS_OPEN)))));

		assertThat(sendEmailAction.conditionsFulfilled(errand, config)).isTrue();
	}

	@Test
	void conditionsFulfilledWhenStatusDoesNotMatch() {
		var errand = ErrandEntity.create().withStatus(STATUS_CLOSED).withLabels(List.of());
		var config = ActionConfigEntity.create();
		config.setConditions(new ArrayList<>(List.of(
			ActionConfigConditionEntity.create().withKey("status").withValues(List.of(STATUS_OPEN)))));

		assertThat(sendEmailAction.conditionsFulfilled(errand, config)).isFalse();
	}

	@Test
	void conditionsFulfilledWhenNoConditions() {
		var errand = ErrandEntity.create().withStatus(STATUS_OPEN).withLabels(List.of());
		var config = ActionConfigEntity.create();
		config.setConditions(new ArrayList<>());

		assertThat(sendEmailAction.conditionsFulfilled(errand, config)).isTrue();
	}

	// validForOperationType tests
	@Test
	void validForOperationTypeCreate() {
		assertThat(sendEmailAction.validForOperationType(OperationType.CREATE)).isTrue();
	}

	@Test
	void validForOperationTypeUpdate() {
		assertThat(sendEmailAction.validForOperationType(OperationType.UPDATE)).isTrue();
	}

	@Test
	void validForOperationTypeDelete() {
		assertThat(sendEmailAction.validForOperationType(OperationType.DELETE)).isFalse();
	}

	@Test
	void validForOperationTypeRead() {
		assertThat(sendEmailAction.validForOperationType(OperationType.READ)).isFalse();
	}
}
