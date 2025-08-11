package se.sundsvall.supportmanagement.integration.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeSyncEntity;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class MessageExchangeSyncRepositoryTest {

	@Autowired
	private MessageExchangeSyncRepository repository;

	@Test
	void findByActive() {
		assertThat(repository.findByActive(true)).hasSize(1).extracting(MessageExchangeSyncEntity::getId).containsExactly(1L);
		assertThat(repository.findByActive(false)).hasSize(1).extracting(MessageExchangeSyncEntity::getId).containsExactly(2L);
	}

	@Test
	void findByMunicipalityId() {
		assertThat(repository.findByMunicipalityId("2281")).hasSize(2).extracting(MessageExchangeSyncEntity::getId).containsExactly(1L, 2L);
		assertThat(repository.findByMunicipalityId("1234")).hasSize(0);
	}

	@Test
	void findByIdAndMunicipalityId() {
		assertThat(repository.findByIdAndMunicipalityId(1L, "2281")).isNotEmpty();
		assertThat(repository.findByIdAndMunicipalityId(1L, "1234")).isEmpty();
		assertThat(repository.findByIdAndMunicipalityId(3L, "2281")).isEmpty();
	}
}
