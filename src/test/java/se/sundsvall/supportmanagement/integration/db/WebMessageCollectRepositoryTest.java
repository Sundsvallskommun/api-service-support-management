package se.sundsvall.supportmanagement.integration.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.WebMessageCollectEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class WebMessageCollectRepositoryTest {

	@Autowired
	private WebMessageCollectRepository repository;

	@Test
	void create() {
		var municipalityId = "municipalityId";
		var namespace = "namespace";
		var instance = "instance";
		var familyIds = List.of("1", "2");
		var entity = WebMessageCollectEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withInstance(instance)
			.withFamilyIds(familyIds);

		assertThat(repository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId)).isEmpty();

		repository.save(entity);

		var result = repository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(result.getFirst().getNamespace()).isEqualTo(namespace);
		assertThat(result.getFirst().getInstance()).isEqualTo(instance);
		assertThat(result.getFirst().getFamilyIds()).containsExactlyInAnyOrderElementsOf(familyIds);

	}

	@Test
	void read() {

		var result = repository.getReferenceById(1L);

		assertThat(result.getMunicipalityId()).isEqualTo("municipality_id-1");
		assertThat(result.getNamespace()).isEqualTo("namespace-1");
		assertThat(result.getInstance()).isEqualTo("instance-1");
		assertThat(result.getFamilyIds()).containsExactlyInAnyOrderElementsOf(List.of("family_id-1", "family_id-2"));
	}

	@Test
	void update() {
		var entity = repository.getReferenceById(1L);

		assertThat(entity.getInstance()).isEqualTo("instance-1");

		entity.setInstance("new-instance");
		repository.save(entity);

		assertThat(repository.getReferenceById(1L).getInstance()).isEqualTo("new-instance");
	}

	@Test
	void findAllByNamespaceAndMunicipalityId() {
		var result = repository.findAllByNamespaceAndMunicipalityId("namespace-1", "municipality_id-1");

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getMunicipalityId()).isEqualTo("municipality_id-1");
		assertThat(result.getFirst().getNamespace()).isEqualTo("namespace-1");
		assertThat(result.getFirst().getInstance()).isEqualTo("instance-1");
		assertThat(result.getFirst().getFamilyIds()).containsExactlyInAnyOrderElementsOf(List.of("family_id-1", "family_id-2"));

		assertThat(result.getLast().getMunicipalityId()).isEqualTo("municipality_id-1");
		assertThat(result.getLast().getNamespace()).isEqualTo("namespace-1");
		assertThat(result.getLast().getInstance()).isEqualTo("instance-2");
		assertThat(result.getLast().getFamilyIds()).containsExactlyInAnyOrderElementsOf(List.of("family_id-3", "family_id-4"));
	}
}
