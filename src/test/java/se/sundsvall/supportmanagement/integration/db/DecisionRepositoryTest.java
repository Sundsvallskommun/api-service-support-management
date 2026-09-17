package se.sundsvall.supportmanagement.integration.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus.DRAFT;

/**
 * The link between a decision and an attachment is a join table, and the queries asking about it traverse the
 * collection - which is what these tests hold them to, beside the queries asking which decision rests on an
 * investigation.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql",
	"/db/scripts/testdata-junit-decision.sql"
})
class DecisionRepositoryTest {

	private static final String NAMESPACE = "NAMESPACE.1";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "ERRAND_ID-2";

	@Autowired
	private DecisionRepository decisionRepository;

	@Test
	@DisplayName("Verification that an attachment is found through any decision of the errand linking it, and through no other errand or tenant")
	void existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsId() {
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "ATTACHMENT_ID-2")).isTrue();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "ATTACHMENT_ID-3")).isTrue();

		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsId(NAMESPACE, MUNICIPALITY_ID, "ERRAND_ID-1", "ATTACHMENT_ID-1")).isFalse();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsId(NAMESPACE, MUNICIPALITY_ID, "ERRAND_ID-1", "ATTACHMENT_ID-2")).isFalse();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsId("NAMESPACE.3", MUNICIPALITY_ID, ERRAND_ID, "ATTACHMENT_ID-2")).isFalse();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsId(NAMESPACE, "2305", ERRAND_ID, "ATTACHMENT_ID-2")).isFalse();
	}

	@Test
	@DisplayName("Verification that the status asked for is that of the decision linking the attachment, not of any decision of the errand")
	void existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsIdAndStatus() {
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsIdAndStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "ATTACHMENT_ID-2", COMPLETED)).isTrue();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsIdAndStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "ATTACHMENT_ID-3", COMPLETED)).isFalse();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsIdAndStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "ATTACHMENT_ID-3", DRAFT)).isTrue();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsIdAndStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "ATTACHMENT_ID-2", DRAFT)).isFalse();
	}

	@Test
	@DisplayName("Verification that an investigation is found through any decision of the errand resting on it, and through no other errand or tenant")
	void existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityId() {
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "investigation-completed")).isTrue();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "investigation-draft")).isTrue();

		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "investigation-unused")).isFalse();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityId(NAMESPACE, MUNICIPALITY_ID, "ERRAND_ID-1", "investigation-completed")).isFalse();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityId(NAMESPACE, "2305", ERRAND_ID, "investigation-completed")).isFalse();
	}

	@Test
	@DisplayName("Verification that the status asked for is that of the decision resting on the investigation")
	void existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityIdAndStatus() {
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityIdAndStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "investigation-completed", COMPLETED))
			.isTrue();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityIdAndStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "investigation-draft", COMPLETED)).isFalse();
		assertThat(decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityIdAndStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "investigation-draft", DRAFT)).isTrue();
	}
}
