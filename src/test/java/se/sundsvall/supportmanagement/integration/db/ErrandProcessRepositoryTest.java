package se.sundsvall.supportmanagement.integration.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;

/**
 * The errand in the test data has been through one process and is in the middle of another, which is the shape the
 * queries have to cope with: one live instance among several dead ones.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql",
	"/db/scripts/testdata-junit-process.sql"
})
class ErrandProcessRepositoryTest {

	@Autowired
	private ErrandProcessRepository errandProcessRepository;

	@Test
	@DisplayName("Verification that the live instance is found among the finished ones, and that one merely waiting counts as live")
	void findByErrandIdAndActiveMarkerIsNotNull() {
		assertThat(errandProcessRepository.findByErrandIdAndActiveMarkerIsNotNull("ERRAND_ID-1"))
			.get()
			.satisfies(entity -> {
				assertThat(entity.getId()).isEqualTo("ep-live-1");
				assertThat(entity.getProcessStatus()).isEqualTo(WAITING);
			});
	}

	@Test
	@DisplayName("Verification that an errand whose only instance has ended has no live one")
	void findByErrandIdAndActiveMarkerIsNotNullWhenNothingIsRunning() {
		assertThat(errandProcessRepository.findByErrandIdAndActiveMarkerIsNotNull("ERRAND_ID-2")).isEmpty();
	}

	@Test
	void findByProcessInstanceId() {
		assertThat(errandProcessRepository.findByProcessInstanceId("pi-live-1"))
			.get()
			.extracting(ErrandProcessEntity::getId)
			.isEqualTo("ep-live-1");
	}

	@Test
	@DisplayName("Verification that the history of an errand comes back newest first")
	void findByErrandIdAndMunicipalityIdAndNamespaceOrderByCreatedDesc() {
		assertThat(errandProcessRepository.findByErrandIdAndMunicipalityIdAndNamespaceOrderByCreatedDesc("ERRAND_ID-1", "2281", "NAMESPACE.1", PageRequest.of(0, 10)))
			.extracting(ErrandProcessEntity::getId)
			.containsExactly("ep-live-1", "ep-done-1");
	}

	@Test
	@DisplayName("Verification that a process which has run its course is remembered, so an ordinary errand change cannot start it over")
	void existsByErrandIdAndProcessStatus() {
		assertThat(errandProcessRepository.existsByErrandIdAndProcessStatus("ERRAND_ID-1", COMPLETED)).isTrue();
		assertThat(errandProcessRepository.existsByErrandIdAndProcessStatus("ERRAND_ID-2", COMPLETED)).isFalse();
	}
}
