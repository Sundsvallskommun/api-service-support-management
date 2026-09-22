package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;

/**
 * Verifies the queries of {@link ErrandProcessRepository}. The errand in the test data has been through one process and
 * is in the middle of another: one live instance among several dead ones.
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
	@DisplayName("Verification that an instance is found through its own errand only, so that narrowing the log of an errand to an instance of another reaches nothing")
	void findByProcessInstanceIdAndErrandId() {
		assertThat(errandProcessRepository.findByProcessInstanceIdAndErrandId("pi-live-1", "ERRAND_ID-1"))
			.get()
			.extracting(ErrandProcessEntity::getId)
			.isEqualTo("ep-live-1");
		assertThat(errandProcessRepository.findByProcessInstanceIdAndErrandId("pi-live-1", "ERRAND_ID-2")).isEmpty();
	}

	@Test
	@DisplayName("Verification that the history of an errand comes back newest first, live and finished instances alike")
	void findByErrandIdOrderByCreatedDesc() {
		assertThat(errandProcessRepository.findByErrandIdOrderByCreatedDesc("ERRAND_ID-1"))
			.extracting(ErrandProcessEntity::getId, ErrandProcessEntity::getProcessStatus)
			.containsExactly(tuple("ep-live-1", WAITING), tuple("ep-done-1", COMPLETED));
	}

	@Test
	@DisplayName("Verification that the instances of several errands come back newest first, so the first row seen per errand is its latest")
	void findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc() {
		assertThat(errandProcessRepository.findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(List.of("ERRAND_ID-1", "ERRAND_ID-2"), "2281", "NAMESPACE.1"))
			.extracting(ErrandProcessEntity::getId)
			.containsExactly("ep-live-1", "ep-failed-2", "ep-done-1");
	}

	@Test
	@DisplayName("Verification that errand ids from another tenant reach nothing")
	void findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDescOfAnotherTenant() {
		assertThat(errandProcessRepository.findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(List.of("ERRAND_ID-1"), "2281", "NAMESPACE.2")).isEmpty();
		assertThat(errandProcessRepository.findByErrandIdInAndMunicipalityIdAndNamespaceOrderByCreatedDesc(List.of("ERRAND_ID-1"), "2262", "NAMESPACE.1")).isEmpty();
	}
}
