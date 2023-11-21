package se.sundsvall.supportmanagement.integration.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.supportmanagement.integration.db.model.ErrandNumberSequenceEntity;

/**
 * Tag ErrandNumberSequenceRepository tests.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class ErrandNumberSequenceRepositoryTest {

	@Autowired
	private ErrandNumberSequenceRepository errandNumberSequenceRepository;

	@Test
	void create() {

		final var lastSequenceNumber = 1;
		final var resetYearMonth = "2101";
		final var namespace = "namespace";

		final var entity = ErrandNumberSequenceEntity.create()
			.withNamespace(namespace)
			.withLastSequenceNumber(lastSequenceNumber)
			.withResetYearMonth(resetYearMonth);

		final var result = errandNumberSequenceRepository.save(entity);


		assertThat(result).isNotNull();
		assertThat(result.getNamespace()).isNotNull().isEqualTo(namespace);
		assertThat(result.getLastSequenceNumber()).isEqualTo(lastSequenceNumber);
		assertThat(result.getResetYearMonth()).isEqualTo(resetYearMonth);
	}

	@Test
	void update() {

		final var lastSequenceNumber = 1;
		final var resetYearMonth = "2101";
		final var namespace = "namespace";

		final var entity = ErrandNumberSequenceEntity.create()
			.withNamespace(namespace)
			.withLastSequenceNumber(lastSequenceNumber)
			.withResetYearMonth(resetYearMonth);

		final var result = errandNumberSequenceRepository.save(entity);

		assertThat(result).isNotNull();
		assertThat(result.getNamespace()).isEqualTo(namespace);
		assertThat(result.getLastSequenceNumber()).isEqualTo(lastSequenceNumber);
		assertThat(result.getResetYearMonth()).isEqualTo(resetYearMonth);

		final var updatedLastSequenceNumber = 2;
		final var updatedResetYearMonth = "2102";

		result.setLastSequenceNumber(updatedLastSequenceNumber);
		result.setResetYearMonth(updatedResetYearMonth);

		final var updatedResult = errandNumberSequenceRepository.save(result);

		assertThat(updatedResult).isNotNull();
		assertThat(updatedResult.getNamespace()).isNotNull();
		assertThat(updatedResult.getLastSequenceNumber()).isEqualTo(updatedLastSequenceNumber);
		assertThat(updatedResult.getResetYearMonth()).isEqualTo(updatedResetYearMonth);

	}

	@Test
	void delete() {

		final var lastSequenceNumber = 1;
		final var resetYearMonth = "2101";
		final var namespace = "namespace";

		final var entity = ErrandNumberSequenceEntity.create()
			.withNamespace(namespace)
			.withLastSequenceNumber(lastSequenceNumber)
			.withResetYearMonth(resetYearMonth);

		final var result = errandNumberSequenceRepository.save(entity);

		errandNumberSequenceRepository.delete(result);

		assertThat(errandNumberSequenceRepository.findById(result.getNamespace())).isEmpty();
	}

}
