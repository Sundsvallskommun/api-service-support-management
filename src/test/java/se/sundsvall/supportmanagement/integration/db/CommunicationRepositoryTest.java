package se.sundsvall.supportmanagement.integration.db;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEmailHeaderEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;
import se.sundsvall.supportmanagement.integration.db.model.enums.Direction;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class CommunicationRepositoryTest {

	private static final String NAMESPACE = "NAMESPACE.1";
	private static final String MUNICIPALITY_ID = "2281";

	@Autowired
	private CommunicationRepository communicationRepository;

	@Test
	void create() {
		// Setup
		final var communicationEntity = CommunicationEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("namespace")
			.withErrandAttachments(List.of(AttachmentEntity.create()))
			.withEmailHeaders(List.of(CommunicationEmailHeaderEntity.create()))
			.withSender("sender")
			.withSenderUserId("senderUserId")
			.withErrandNumber("errandNumber")
			.withDirection(Direction.INBOUND)
			.withExternalId("externalCaseID")
			.withSubject("subject")
			.withMessageBody("messageBody")
			.withHtmlMessageBody("htmlMessageBody")
			.withSent(OffsetDateTime.now())
			.withType(CommunicationType.EMAIL)
			.withTarget("target")
			.withRecipients(List.of("recipient"))
			.withCcRecipients(List.of("ccRecipient"))
			.withViewed(true)
			.withInternal(true)
			.withAttachments(List.of(CommunicationAttachmentEntity.create()));

		// Execution
		final var persistedEntity = communicationRepository.save(communicationEntity);

		// Assertions
		assertThat(persistedEntity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(persistedEntity.getSender()).isEqualTo("sender");
		assertThat(persistedEntity.getSenderUserId()).isEqualTo("senderUserId");
		assertThat(persistedEntity.getErrandNumber()).isEqualTo("errandNumber");
		assertThat(persistedEntity.getDirection()).isEqualTo(Direction.INBOUND);
		assertThat(persistedEntity.getExternalId()).isEqualTo("externalCaseID");
		assertThat(persistedEntity.getSubject()).isEqualTo("subject");
		assertThat(persistedEntity.getMessageBody()).isEqualTo("messageBody");
		assertThat(persistedEntity.getHtmlMessageBody()).isEqualTo("htmlMessageBody");
		assertThat(persistedEntity.getSent()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(persistedEntity.getType()).isEqualTo(CommunicationType.EMAIL);
		assertThat(persistedEntity.getTarget()).isEqualTo("target");
		assertThat(persistedEntity.isViewed()).isTrue();
		assertThat(persistedEntity.getAttachments()).hasSize(1);
		assertThat(persistedEntity.getEmailHeaders()).hasSize(1);
		assertThat(persistedEntity.getErrandAttachments()).hasSize(1);
		assertThat(persistedEntity.getNamespace()).isEqualTo("namespace");
		assertThat(persistedEntity.getMunicipalityId()).isEqualTo("2281");
		assertThat(persistedEntity.isInternal()).isTrue();
	}

	@Test
	void findByErrandNumberAndNamespaceAndMunicipalityId() {
		// Setup
		final var errandNumber = "errand1";

		// Execution
		final var communications = communicationRepository.findByErrandNumberAndNamespaceAndMunicipalityId(errandNumber, NAMESPACE, MUNICIPALITY_ID);

		// Assertions
		assertThat(communications).isNotEmpty();
		assertThat(communications.getFirst().getErrandNumber()).isEqualTo(errandNumber);
	}

	@Test
	void delete() {
		// Setup
		final var communicationEntity = communicationRepository.findById("comm1").orElseThrow();

		// Execution
		communicationRepository.delete(communicationEntity);

		// Assertions
		assertThat(communicationRepository.findById("communicationID")).isNotPresent();
	}

	@Test
	void existsByErrandNumberAndNamespaceAndMunicipalityIdAndExternalId() {
		assertThat(communicationRepository.existsByErrandNumberAndNamespaceAndMunicipalityIdAndExternalId("errand1", NAMESPACE, MUNICIPALITY_ID, "case1")).isTrue();
		assertThat(communicationRepository.existsByErrandNumberAndNamespaceAndMunicipalityIdAndExternalId("errand1", NAMESPACE, MUNICIPALITY_ID, "case2")).isFalse();
	}

	@Test
	void findByErrandNumberAndNamespaceAndMunicipalityIdAndInternal() {
		final var communications = communicationRepository.findByErrandNumberAndNamespaceAndMunicipalityIdAndInternal("errand1", NAMESPACE, MUNICIPALITY_ID, true);
		assertThat(communications).isNotEmpty().hasSize(1).allMatch(CommunicationEntity::isInternal);
	}
}
