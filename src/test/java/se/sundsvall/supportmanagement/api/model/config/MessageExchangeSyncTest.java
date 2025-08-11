package se.sundsvall.supportmanagement.api.model.config;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.OffsetDateTime;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MessageExchangeSyncTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(MessageExchangeSync.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		var id = 1L;
		var namespace = "namespace";
		var latestSyncedSequenceNumber = 2L;
		var modified = now();
		var active = true;

		var syncEntity = MessageExchangeSync.create()
			.withId(id)
			.withNamespace(namespace)
			.withLatestSyncedSequenceNumber(latestSyncedSequenceNumber)
			.withModified(modified)
			.withActive(active);

		Assertions.assertThat(syncEntity).hasNoNullFieldsOrProperties();
		Assertions.assertThat(syncEntity.getId()).isEqualTo(id);
		Assertions.assertThat(syncEntity.getNamespace()).isEqualTo(namespace);
		Assertions.assertThat(syncEntity.getLatestSyncedSequenceNumber()).isEqualTo(latestSyncedSequenceNumber);
		Assertions.assertThat(syncEntity.getModified()).isEqualTo(modified);
		Assertions.assertThat(syncEntity.getActive()).isEqualTo(active);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		Assertions.assertThat(MessageExchangeSync.create()).hasAllNullFieldsOrProperties();
		Assertions.assertThat(new MessageExchangeSync()).hasAllNullFieldsOrProperties();
	}
}
