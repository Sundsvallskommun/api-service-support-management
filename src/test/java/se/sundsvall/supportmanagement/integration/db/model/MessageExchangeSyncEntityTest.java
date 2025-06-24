package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MessageExchangeSyncEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(MessageExchangeSyncEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		var id = 1L;
		var namespace = "namespace";
		var municipalityId = "municipalityId";
		var latestSyncedSequenceNumber = 2L;
		var modified = now();
		var active = true;

		var syncEntity = MessageExchangeSyncEntity.create()
			.withId(id)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withLatestSyncedSequenceNumber(latestSyncedSequenceNumber)
			.withModified(modified)
			.withActive(active);

		assertThat(syncEntity).hasNoNullFieldsOrProperties();
		assertThat(syncEntity.getId()).isEqualTo(id);
		assertThat(syncEntity.getNamespace()).isEqualTo(namespace);
		assertThat(syncEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(syncEntity.getLatestSyncedSequenceNumber()).isEqualTo(latestSyncedSequenceNumber);
		assertThat(syncEntity.getModified()).isEqualTo(modified);
		assertThat(syncEntity.isActive()).isEqualTo(active);
	}
}
