package se.sundsvall.supportmanagement.api.model.communication.conversation;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ConversationTest {

	@Test
	void testBean() {
		assertThat(Conversation.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var id = UUID.randomUUID().toString();
		final var participants = List.of(Identifier.create());
		final var relationIds = List.of(UUID.randomUUID().toString());
		final var topic = "topic";
		final var type = ConversationType.INTERNAL;
		final var metadata = List.of(KeyValues.create());

		final var object = Conversation.create()
			.withId(id)
			.withParticipants(participants)
			.withRelationIds(relationIds)
			.withTopic(topic)
			.withType(type)
			.withMetadata(metadata);

		assertThat(object).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(object.getId()).isEqualTo(id);
		assertThat(object.getParticipants()).isEqualTo(participants);
		assertThat(object.getRelationIds()).isEqualTo(relationIds);
		assertThat(object.getTopic()).isEqualTo(topic);
		assertThat(object.getType()).isEqualTo(type);
		assertThat(object.getMetadata()).isEqualTo(metadata);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Conversation.create()).hasAllNullFieldsOrProperties();
		assertThat(new Conversation()).hasAllNullFieldsOrProperties();
	}
}
