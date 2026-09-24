package se.sundsvall.supportmanagement.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.ConversationService;
import se.sundsvall.supportmanagement.service.ErrandAccessService;
import se.sundsvall.supportmanagement.service.ErrandActionService;
import se.sundsvall.supportmanagement.service.ErrandAttachmentService;
import se.sundsvall.supportmanagement.service.ErrandDecisionService;
import se.sundsvall.supportmanagement.service.ErrandInvestigationService;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService;
import se.sundsvall.supportmanagement.service.ErrandMeasureService;
import se.sundsvall.supportmanagement.service.ErrandNoteService;
import se.sundsvall.supportmanagement.service.ErrandParameterService;
import se.sundsvall.supportmanagement.service.ErrandProcessService;
import se.sundsvall.supportmanagement.service.ErrandPurgeService;
import se.sundsvall.supportmanagement.service.ErrandService;
import se.sundsvall.supportmanagement.service.ErrandStatementService;
import se.sundsvall.supportmanagement.service.EventService;
import se.sundsvall.supportmanagement.service.HandoverPreviewService;
import se.sundsvall.supportmanagement.service.HandoverService;
import se.sundsvall.supportmanagement.service.JobService;
import se.sundsvall.supportmanagement.service.MetadataService;
import se.sundsvall.supportmanagement.service.NotificationService;
import se.sundsvall.supportmanagement.service.ProcessCommandService;
import se.sundsvall.supportmanagement.service.RevisionService;
import se.sundsvall.supportmanagement.service.SubscriberNotificationService;
import se.sundsvall.supportmanagement.service.SubscriberService;
import se.sundsvall.supportmanagement.service.SubscriptionService;
import se.sundsvall.supportmanagement.service.TimeMeasurementService;
import se.sundsvall.supportmanagement.service.config.EmailIntegrationConfigService;
import se.sundsvall.supportmanagement.service.config.MessageExchangeIntegrationConfigService;
import se.sundsvall.supportmanagement.service.config.MessageExchangeSyncConfigService;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.config.ValidationService;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * A test of the API layer alone: the application on a random port, a WebTestClient, and every service a resource
 * talks to replaced by a mock, reset after each test. A test reaches a mock by autowiring its type.
 * <p>
 * Every class carrying it shares one application context. A service added to a resource has to be added here as well,
 * or its test meets the real bean.
 */
@Target(TYPE)
@Retention(RUNTIME)
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
@MockitoBean(types = {
	AccessControlService.class,
	CommunicationService.class,
	ConversationService.class,
	EmailIntegrationConfigService.class,
	ErrandAccessService.class,
	ErrandActionService.class,
	ErrandAttachmentService.class,
	ErrandDecisionService.class,
	ErrandInvestigationService.class,
	ErrandJsonParameterService.class,
	ErrandMeasureService.class,
	ErrandNoteService.class,
	ErrandParameterService.class,
	ErrandProcessService.class,
	ErrandPurgeService.class,
	ErrandService.class,
	ErrandStatementService.class,
	EventService.class,
	HandoverPreviewService.class,
	HandoverService.class,
	JobService.class,
	JsonSchemaClient.class,
	MessageExchangeIntegrationConfigService.class,
	MessageExchangeSyncConfigService.class,
	MetadataService.class,
	NamespaceConfigService.class,
	NotificationService.class,
	ProcessCommandService.class,
	RevisionService.class,
	SubscriberNotificationService.class,
	SubscriberService.class,
	SubscriptionService.class,
	TimeMeasurementService.class,
	ValidationService.class
})
public @interface ResourceTest {
}
