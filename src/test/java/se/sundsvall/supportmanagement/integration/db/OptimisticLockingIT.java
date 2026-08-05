package se.sundsvall.supportmanagement.integration.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
@Transactional
class OptimisticLockingIT {

	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String PARAMETER_ID = "45d266a7-1ff2-4bf4-b6f3-0473b2b86fcd";
	private static final String JSON_PARAMETER_ID = "aabb1234-0001-0001-0001-000000000001";

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void shouldThrowOnConcurrentErrandModification() {
		final var errand = errandsRepository.findById(ERRAND_ID).orElseThrow();
		assertThat(errand.getVersion()).isZero();

		jdbcTemplate.update("UPDATE errand SET version = version + 1 WHERE id = ?", ERRAND_ID);
		errand.setTitle("concurrent modification");

		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
			.isThrownBy(() -> errandsRepository.saveAndFlush(errand));
	}

	@Test
	void shouldThrowOnConcurrentParameterModification() {
		final var parameter = parameterRepository.findById(PARAMETER_ID).orElseThrow();
		assertThat(parameter.getVersion()).isZero();

		jdbcTemplate.update("UPDATE parameter SET version = version + 1 WHERE id = ?", PARAMETER_ID);
		parameter.setDisplayName("concurrent modification");

		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
			.isThrownBy(() -> parameterRepository.saveAndFlush(parameter));
	}

	@Test
	void shouldThrowOnConcurrentJsonParameterModification() {
		final var errand = errandsRepository.findById(ERRAND_ID).orElseThrow();
		final var jsonParameter = errand.getJsonParameters().stream()
			.filter(jp -> "formData".equals(jp.getKey()))
			.findFirst()
			.orElseThrow();
		assertThat(jsonParameter.getVersion()).isZero();

		jdbcTemplate.update("UPDATE json_parameter SET version = version + 1 WHERE id = ?", JSON_PARAMETER_ID);
		jsonParameter.setValue("{\"firstName\":\"concurrent modification\"}");

		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
			.isThrownBy(() -> errandsRepository.saveAndFlush(errand));
	}
}
