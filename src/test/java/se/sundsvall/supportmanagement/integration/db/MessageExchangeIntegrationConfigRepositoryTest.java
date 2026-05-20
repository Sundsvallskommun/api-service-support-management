package se.sundsvall.supportmanagement.integration.db;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeIntegrationConfigEntity;

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
class MessageExchangeIntegrationConfigRepositoryTest {

	@Autowired
	private MessageExchangeIntegrationConfigRepository repository;

	@Test
	void create() {
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var triggerStatusChangeOn = "SOLVED";
		final var statusChangeTo = "OPEN";

		final var entity = MessageExchangeIntegrationConfigEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withTriggerStatusChangeOn(triggerStatusChangeOn)
			.withStatusChangeTo(statusChangeTo);

		assertThat(repository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).isFalse();

		repository.save(entity);

		assertThat(repository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).isTrue();

		final var result = repository.getByNamespaceAndMunicipalityId(namespace, municipalityId);
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isNotNull();
		assertThat(result.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(result.get().getNamespace()).isEqualTo(namespace);
		assertThat(result.get().getTriggerStatusChangeOn()).isEqualTo(triggerStatusChangeOn);
		assertThat(result.get().getStatusChangeTo()).isEqualTo(statusChangeTo);
		assertThat(result.get().getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(result.get().getModified()).isNull();
	}

	@Test
	void update() {
		final var optionalEntity = repository.getByNamespaceAndMunicipalityId("namespace-1", "2281");

		assertThat(optionalEntity).isPresent();
		final var entity = optionalEntity.get();
		assertThat(entity.getStatusChangeTo()).isEqualTo("OPEN");

		entity.withStatusChangeTo("NEW");
		repository.saveAndFlush(entity);

		final var modifiedEntity = repository.getByNamespaceAndMunicipalityId("namespace-1", "2281");
		assertThat(modifiedEntity).isPresent();
		assertThat(modifiedEntity.get().getStatusChangeTo()).isEqualTo("NEW");
		assertThat(modifiedEntity.get().getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
	}

	@Test
	void getByNamespaceAndMunicipalityId() {
		final var result = repository.getByNamespaceAndMunicipalityId("namespace-1", "2281");
		assertThat(result).isPresent();
	}

	@Test
	void existsByNamespaceAndMunicipalityId() {
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-1", "2281")).isTrue();
	}

	@Test
	void delete() {
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-1", "2281")).isTrue();
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-2", "2282")).isTrue();

		repository.deleteByNamespaceAndMunicipalityId("namespace-1", "2281");

		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-1", "2281")).isFalse();
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-2", "2282")).isTrue();
	}
}
