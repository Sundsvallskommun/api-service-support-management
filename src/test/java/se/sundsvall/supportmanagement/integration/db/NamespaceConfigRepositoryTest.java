package se.sundsvall.supportmanagement.integration.db;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class NamespaceConfigRepositoryTest {

	@Autowired
	private NamespaceConfigRepository repository;

	@Test
	void getByNamespaceAndMunicipalityId() {
		final var result = repository.getByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1");
		assertThat(result).isPresent();
	}

	@Test
	void existsByNamespaceAndMunicipalityId() {
		assertThat(repository.existsByNamespaceAndMunicipalityId("NAMEspace-1", "municipality_id-1")).isTrue();
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-99", "municipality_id-99")).isFalse();
	}

	@Test
	void findAllByMunicipalityId() {
		final var municipalityId = "municipality_id-1";
		final var result = repository.findAllByMunicipalityId(municipalityId);

		assertThat(result).hasSize(2).satisfiesExactlyInAnyOrder(bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
			assertThat(bean.getNamespace()).isEqualTo("namespace-1");
			assertThat(bean.getDisplayName()).isEqualTo("display name 1");
			assertThat(bean.getShortCode()).isEqualTo("short_code-1");
		}, bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
			assertThat(bean.getNamespace()).isEqualTo("namespace-3");
			assertThat(bean.getDisplayName()).isEqualTo("display name 3");
			assertThat(bean.getShortCode()).isEqualTo("short_code-3");
		});
	}

	@Test
	void findAll() {
		final var result = repository.findAll();

		assertThat(result).hasSize(3).satisfiesExactlyInAnyOrder(bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo("municipality_id-1");
			assertThat(bean.getNamespace()).isEqualTo("namespace-1");
			assertThat(bean.getDisplayName()).isEqualTo("display name 1");
			assertThat(bean.getShortCode()).isEqualTo("short_code-1");
		}, bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo("municipality_id-2");
			assertThat(bean.getNamespace()).isEqualTo("namespace-2");
			assertThat(bean.getDisplayName()).isEqualTo("display name 2");
			assertThat(bean.getShortCode()).isEqualTo("short_code-2");
		}, bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo("municipality_id-1");
			assertThat(bean.getNamespace()).isEqualTo("namespace-3");
			assertThat(bean.getDisplayName()).isEqualTo("display name 3");
			assertThat(bean.getShortCode()).isEqualTo("short_code-3");
		});
	}

	@Test
	void create() {

		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var displayName = "displayName";
		final var shortCode = "shortCode";

		final var entity = NamespaceConfigEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withDisplayName(displayName)
			.withShortCode(shortCode);

		assertThat(repository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).isFalse();

		repository.saveAndFlush(entity);

		final var result = repository.getByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(result).isPresent().get().satisfies(bean -> {
			assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
			assertThat(bean.getNamespace()).isEqualTo(namespace);
			assertThat(bean.getDisplayName()).isEqualTo(displayName);
			assertThat(bean.getShortCode()).isEqualTo(shortCode);
			assertThat(bean.getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
			assertThat(bean.getModified()).isNull();
		});
	}

	@Test
	void update() {
		final var entity = repository.getByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1")
			.orElseThrow(() -> new RuntimeException("Missing data in /db/scripts/testdata-junit.sql"));

		assertThat(entity.getDisplayName()).isEqualTo("display name 1");
		assertThat(entity.getShortCode()).isEqualTo("short_code-1");

		repository.saveAndFlush(entity
			.withDisplayName("new displayname")
			.withShortCode("newCode"));

		final var modifiedEntity = repository.getByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1");
		assertThat(modifiedEntity).isPresent().get().satisfies(bean -> {
			assertThat(bean.getDisplayName()).isEqualTo("new displayname");
			assertThat(bean.getShortCode()).isEqualTo("newCode");
			assertThat(bean.getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		});
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
