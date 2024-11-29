package se.sundsvall.supportmanagement.integration.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;

import java.time.OffsetDateTime;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class EmailWorkerConfigRepositoryTest {

	@Autowired
	private EmailWorkerConfigRepository repository;

	@Test
	void create() {

		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var enabled = true;
		final var errandClosedEmailSender = "noreply@email.se";
		final var errandClosedEmailTemplate = "This is an email";
		final var errandNewEmailSender = "test@email.se";
		final var errandNewEmailTemplate = "This is an email too";
		final var daysOfInactivityBeforeReject = 3;
		final var statusForNew = "NEW";
		final var triggerStatusChangeOn = "SOLVED";
		final var statusChangeTo = "OPEN";
		final var inactiveStatus = "CLOSED";
		final var addSenderAsStakeholder = true;
		final var stakeholderRole = "stakeholderRole";
		final var errandChannel = "errandChannel";

		final var entity = EmailWorkerConfigEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withEnabled(enabled)
			.withErrandClosedEmailSender(errandClosedEmailSender)
			.withErrandClosedEmailTemplate(errandClosedEmailTemplate)
			.withErrandNewEmailSender(errandNewEmailSender)
			.withErrandNewEmailTemplate(errandNewEmailTemplate)
			.withDaysOfInactivityBeforeReject(daysOfInactivityBeforeReject)
			.withStatusForNew(statusForNew)
			.withTriggerStatusChangeOn(triggerStatusChangeOn)
			.withStatusChangeTo(statusChangeTo)
			.withInactiveStatus(inactiveStatus)
			.withAddSenderAsStakeholder(addSenderAsStakeholder)
			.withStakeholderRole(stakeholderRole)
			.withErrandChannel(errandChannel);

		assertThat(repository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).isFalse();

		repository.save(entity);

		assertThat(repository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).isTrue();

		final var result = repository.getByNamespaceAndMunicipalityId(namespace, municipalityId);
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isNotNull();
		assertThat(result.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(result.get().getNamespace()).isEqualTo(namespace);
		assertThat(result.get().getEnabled()).isEqualTo(enabled);
		assertThat(result.get().getErrandClosedEmailSender()).isEqualTo(errandClosedEmailSender);
		assertThat(result.get().getErrandClosedEmailTemplate()).isEqualTo(errandClosedEmailTemplate);
		assertThat(result.get().getErrandNewEmailSender()).isEqualTo(errandNewEmailSender);
		assertThat(result.get().getErrandNewEmailTemplate()).isEqualTo(errandNewEmailTemplate);
		assertThat(result.get().getDaysOfInactivityBeforeReject()).isEqualTo(daysOfInactivityBeforeReject);
		assertThat(result.get().getStatusForNew()).isEqualTo(statusForNew);
		assertThat(result.get().getTriggerStatusChangeOn()).isEqualTo(triggerStatusChangeOn);
		assertThat(result.get().getStatusChangeTo()).isEqualTo(statusChangeTo);
		assertThat(result.get().getInactiveStatus()).isEqualTo(inactiveStatus);
		assertThat(result.get().isAddSenderAsStakeholder()).isEqualTo(addSenderAsStakeholder);
		assertThat(result.get().getStakeholderRole()).isEqualTo(stakeholderRole);
		assertThat(result.get().getErrandChannel()).isEqualTo(errandChannel);
		assertThat(result.get().getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(result.get().getModified()).isNull();
	}

	@Test
	void update() {
		final var optionalEntity = repository.getByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1");

		assertThat(optionalEntity).isPresent();
		final var entity = optionalEntity.get();
		assertThat(entity.getEnabled()).isTrue();

		entity.withEnabled(false);
		repository.saveAndFlush(entity);

		final var modifiedEntity = repository.getByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1");
		assertThat(modifiedEntity).isPresent();
		assertThat(modifiedEntity.get().getEnabled()).isFalse();
		assertThat(modifiedEntity.get().getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));

	}

	@Test
	void getByNamespaceAndMunicipalityId() {
		final var result = repository.getByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1");
		assertThat(result).isPresent();
	}

	@Test
	void existsByNamespaceAndMunicipalityId() {
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1")).isTrue();
	}

	@Test
	void delete() {
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1")).isTrue();
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-2", "municipality_id-2")).isTrue();

		repository.deleteByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1");

		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1")).isFalse();
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-2", "municipality_id-2")).isTrue();
	}
}
