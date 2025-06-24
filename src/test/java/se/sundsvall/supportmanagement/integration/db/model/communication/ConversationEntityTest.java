package se.sundsvall.supportmanagement.integration.db.model.communication;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConversationEntityTest {

	@Test
	void testBean() {
		assertThat(ConversationEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = "id";
		final var messageExchangeId = "messageExchangeId";
		final var errandId = "errandId";
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var topic = "topic";
		final var type = "type";
		final var relationIds = List.of("relationId");
		final var latestSyncedSequenceNumber = 123L;
		final var targetRelationId = "targetRelationId";

		final var entity = ConversationEntity.create()
			.withId(id)
			.withMessageExchangeId(messageExchangeId)
			.withErrandId(errandId)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withTopic(topic)
			.withType(type)
			.withRelationIds(relationIds)
			.withLatestSyncedSequenceNumber(latestSyncedSequenceNumber)
			.withTargetRelationId(targetRelationId);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMessageExchangeId()).isEqualTo(messageExchangeId);
		assertThat(entity.getErrandId()).isEqualTo(errandId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getTopic()).isEqualTo(topic);
		assertThat(entity.getType()).isEqualTo(type);
		assertThat(entity.getRelationIds()).isEqualTo(relationIds);
		assertThat(entity.getLatestSyncedSequenceNumber()).isEqualTo(latestSyncedSequenceNumber);
		assertThat(entity.getTargetRelationId()).isEqualTo(targetRelationId);
	}
}
