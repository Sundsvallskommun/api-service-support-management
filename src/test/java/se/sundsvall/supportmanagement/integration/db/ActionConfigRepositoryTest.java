package se.sundsvall.supportmanagement.integration.db;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class ActionConfigRepositoryTest {

	@Autowired
	private ActionConfigRepository repository;

	@Test
	void findAllByNamespaceAndMunicipalityId() {
		final var result = repository.findAllByNamespaceAndMunicipalityId("namespace-1", "2281");

		assertThat(result).hasSize(2);
		assertThat(result).extracting(ActionConfigEntity::getName).containsExactlyInAnyOrder("ADD_LABEL", "SEND_EMAIL");
	}

	@Test
	void findAllByNamespaceAndMunicipalityIdNotFound() {
		final var result = repository.findAllByNamespaceAndMunicipalityId("namespace-99", "2281");

		assertThat(result).isEmpty();
	}

	@Test
	void findByIdAndNamespaceAndMunicipalityId() {
		final var result = repository.findByIdAndNamespaceAndMunicipalityId("action-config-id-1", "namespace-1", "2281");

		assertThat(result).isPresent().get().satisfies(entity -> {
			assertThat(entity.getName()).isEqualTo("ADD_LABEL");
			assertThat(entity.getActive()).isTrue();
			assertThat(entity.getDisplayValue()).isEqualTo("Label will be added");
			assertThat(entity.getConditions()).hasSize(2);
			assertThat(entity.getParameters()).hasSize(1);
		});
	}

	@Test
	void findByIdAndNamespaceAndMunicipalityIdNotFound() {
		final var result = repository.findByIdAndNamespaceAndMunicipalityId("action-config-id-1", "namespace-2", "2281");

		assertThat(result).isEmpty();
	}

	@Test
	void existsByIdAndNamespaceAndMunicipalityId() {
		assertThat(repository.existsByIdAndNamespaceAndMunicipalityId("action-config-id-1", "namespace-1", "2281")).isTrue();
		assertThat(repository.existsByIdAndNamespaceAndMunicipalityId("action-config-id-1", "namespace-99", "2281")).isFalse();
	}

	@Test
	void create() {
		final var municipalityId = "2281";
		final var namespace = "namespace-new";
		final var name = "NEW_ACTION";
		final var displayValue = "New action display";

		final var condition = ActionConfigConditionEntity.create()
			.withKey("status")
			.withValues(List.of("OPEN", "CLOSED"));

		final var parameter = ActionConfigParameterEntity.create()
			.withKey("target")
			.withValues(List.of("value1"));

		final var entity = ActionConfigEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withName(name)
			.withActive(true)
			.withDisplayValue(displayValue)
			.withConditions(List.of(condition))
			.withParameters(List.of(parameter));

		condition.setActionConfigEntity(entity);
		parameter.setActionConfigEntity(entity);

		repository.saveAndFlush(entity);

		final var result = repository.findByIdAndNamespaceAndMunicipalityId(entity.getId(), namespace, municipalityId);

		assertThat(result).isPresent().get().satisfies(persisted -> {
			assertThat(persisted.getMunicipalityId()).isEqualTo(municipalityId);
			assertThat(persisted.getNamespace()).isEqualTo(namespace);
			assertThat(persisted.getName()).isEqualTo(name);
			assertThat(persisted.getActive()).isTrue();
			assertThat(persisted.getDisplayValue()).isEqualTo(displayValue);
			assertThat(persisted.getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
			assertThat(persisted.getModified()).isNull();
			assertThat(persisted.getConditions()).hasSize(1).first().satisfies(c -> {
				assertThat(c.getKey()).isEqualTo("status");
				assertThat(c.getValues()).containsExactly("OPEN", "CLOSED");
			});
			assertThat(persisted.getParameters()).hasSize(1).first().satisfies(p -> {
				assertThat(p.getKey()).isEqualTo("target");
				assertThat(p.getValues()).containsExactly("value1");
			});
		});
	}

	@Test
	void update() {
		final var entity = repository.findByIdAndNamespaceAndMunicipalityId("action-config-id-1", "namespace-1", "2281")
			.orElseThrow(() -> new RuntimeException("Missing data in /db/scripts/testdata-junit.sql"));

		assertThat(entity.getName()).isEqualTo("ADD_LABEL");

		entity.setName("UPDATED_ACTION");
		entity.setActive(false);

		repository.saveAndFlush(entity);

		final var updated = repository.findByIdAndNamespaceAndMunicipalityId("action-config-id-1", "namespace-1", "2281");

		assertThat(updated).isPresent().get().satisfies(bean -> {
			assertThat(bean.getName()).isEqualTo("UPDATED_ACTION");
			assertThat(bean.getActive()).isFalse();
			assertThat(bean.getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		});
	}

	@Test
	void delete() {
		assertThat(repository.existsByIdAndNamespaceAndMunicipalityId("action-config-id-1", "namespace-1", "2281")).isTrue();

		repository.deleteByIdAndNamespaceAndMunicipalityId("action-config-id-1", "namespace-1", "2281");

		assertThat(repository.existsByIdAndNamespaceAndMunicipalityId("action-config-id-1", "namespace-1", "2281")).isFalse();
		assertThat(repository.existsByIdAndNamespaceAndMunicipalityId("action-config-id-3", "namespace-2", "2282")).isTrue();
	}
}
