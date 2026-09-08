package se.sundsvall.supportmanagement.integration.db;

import jakarta.persistence.EntityManager;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.MeasureTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({ "/db/scripts/truncate.sql", "/db/scripts/testdata-junit.sql" })
class MeasureTypeRoleAssignmentsTest {

	@Autowired
	private MeasureTypeRepository measureTypeRepository;
	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private EntityManager entityManager;

	@Test
	void assignmentsSurviveReloadAndProtectTheirRoleReference() {
		final var role = roleRepository.saveAndFlush(RoleEntity.create().withName("MEASURE_TEST_ROLE").withNamespace("namespace-1").withMunicipalityId("2281"));
		final var type = measureTypeRepository.saveAndFlush(MeasureTypeEntity.create()
			.withName("MEASURE_TEST_TYPE").withMeasureGroup("GROUP").withNamespace("namespace-1").withMunicipalityId("2281")
			.withAllowedRoleIds(Set.of(role.getId())));
		final var typeId = type.getId();
		final var roleId = role.getId();
		entityManager.clear();

		assertThat(measureTypeRepository.findById(typeId).orElseThrow().getAllowedRoleIds()).containsExactly(roleId);
		assertThat(measureTypeRepository.existsByAllowedRoleIdsContaining(roleId)).isTrue();
		assertThatThrownBy(() -> {
			roleRepository.deleteById(roleId);
			roleRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}
}
