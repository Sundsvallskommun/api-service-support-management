package se.sundsvall.supportmanagement.integration.db;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;

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
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1")).isTrue();
		assertThat(repository.existsByNamespaceAndMunicipalityId("namespace-99", "municipality_id-99")).isFalse();
	}

	@Test
	void create() {

		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var shortCode = "shortCode";

		final var entity = NamespaceConfigEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withShortCode(shortCode);

		assertThat(repository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).isFalse();

		repository.saveAndFlush(entity);

		var result = repository.getByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(result).isNotEmpty();
		assertThat(result.get().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(result.get().getNamespace()).isEqualTo(namespace);
		assertThat(result.get().getShortCode()).isEqualTo(shortCode);
		assertThat(result.get().getCreated()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
		assertThat(result.get().getModified()).isNull();
	}

	@Test
	void update() {
		final var entity = repository.getByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1")
			.orElseThrow(() -> new RuntimeException("Missing data in /db/scripts/testdata-junit.sql"));

		assertThat(entity.getShortCode()).isEqualTo("short_code-1");

		repository.saveAndFlush(entity.withShortCode("newCode"));

		final var modifiedEntity = repository.getByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1");
		assertThat(modifiedEntity).hasValueSatisfying(v -> {
			assertThat(v.getShortCode()).isEqualTo("newCode");
			assertThat(v.getModified()).isCloseTo(OffsetDateTime.now(), within(2, SECONDS));
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