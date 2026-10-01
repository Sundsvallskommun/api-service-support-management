package se.sundsvall.supportmanagement.integration.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.LabelClassificationEntity;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql"
})
class LabelClassificationRepositoryTest {

	private static final String NAMESPACE = "namespace-1";
	private static final String MUNICIPALITY_ID = "2281";

	@Autowired
	private LabelClassificationRepository repository;

	@Test
	void createAndFind() {
		repository.saveAndFlush(entity("subtype", "Undertyp"));
		repository.saveAndFlush(entity("category", "Kategori"));
		repository.saveAndFlush(LabelClassificationEntity.create().withNamespace("other-namespace").withMunicipalityId(MUNICIPALITY_ID).withClassification("subtype"));

		final var found = repository.findByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, "subtype");

		assertThat(found).isPresent();
		assertThat(found.get().getId()).isNotNull();
		assertThat(found.get().getDisplayName()).isEqualTo("Undertyp");
		assertThat(found.get().getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(found.get().getModified()).isNull();
		assertThat(repository.existsByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, "subtype")).isTrue();
		assertThat(repository.existsByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, "unknown")).isFalse();
		assertThat(repository.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID, Sort.by("classification")))
			.extracting(LabelClassificationEntity::getClassification)
			.containsExactly("category", "subtype");
	}

	@Test
	void classificationIsUniqueWithinNamespace() {
		repository.saveAndFlush(entity("subtype", "Undertyp"));

		final var duplicate = entity("subtype", "Annan");
		assertThatThrownBy(() -> repository.saveAndFlush(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void delete() {
		repository.saveAndFlush(entity("subtype", "Undertyp"));

		repository.deleteByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, "subtype");

		assertThat(repository.existsByNamespaceAndMunicipalityIdAndClassification(NAMESPACE, MUNICIPALITY_ID, "subtype")).isFalse();
	}

	private static LabelClassificationEntity entity(final String classification, final String displayName) {
		return LabelClassificationEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withClassification(classification)
			.withDisplayName(displayName);
	}
}
