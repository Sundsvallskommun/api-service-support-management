package se.sundsvall.supportmanagement.integration.db;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderParameterEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the stakeholder parameter tables on the schema Flyway migrates under the dbtest profile.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles({
	"junit", "dbtest"
})
@Transactional
class StakeholderParameterDataModelTest {

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("Verification that a stakeholder parameter value of the 3000 characters the API allows is stored whole")
	void aValueOfTheLengthTheApiAllowsIsStoredWhole() {
		final var value = "x".repeat(3000);
		final var errand = ErrandEntity.create()
			.withMunicipalityId("2281")
			.withNamespace("STAKEHOLDER-DATA-MODEL")
			.withErrandNumber("SDM-" + UUID.randomUUID())
			.withTitle("TITLE")
			.withStatus("STATUS")
			.withPriority("MEDIUM")
			.withReporterUserId("joe01doe");
		final var stakeholder = StakeholderEntity.create().withErrandEntity(errand).withRole("APPLICANT");
		stakeholder.setParameters(new ArrayList<>(List.of(StakeholderParameterEntity.create()
			.withStakeholderEntity(stakeholder)
			.withKey("key")
			.withValues(new ArrayList<>(List.of(value))))));
		errand.setStakeholders(new ArrayList<>(List.of(stakeholder)));

		final var errandId = errandsRepository.saveAndFlush(errand).getId();
		entityManager.clear();

		assertThat(errandsRepository.findById(errandId).orElseThrow().getStakeholders().getFirst().getParameters().getFirst().getValues())
			.containsExactly(value);
	}
}
