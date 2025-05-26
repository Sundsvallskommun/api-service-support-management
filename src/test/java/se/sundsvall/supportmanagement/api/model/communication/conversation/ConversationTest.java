package se.sundsvall.supportmanagement.api.model.communication.conversation;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

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
		final var type = ConversationType.D2D;
		final var metadata = List.of(KeyValues.create());
		final var externalReferences = List.of(KeyValues.create());

		final var o = Conversation.create()
			.withId(id)
			.withParticipants(participants)
			.withRelationIds(relationIds)
			.withTopic(topic)
			.withType(type)
			.withExternalReferences(externalReferences)
			.withMetadata(metadata);

		assertThat(o).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(o.getId()).isEqualTo(id);
		assertThat(o.getParticipants()).isEqualTo(participants);
		assertThat(o.getRelationIds()).isEqualTo(relationIds);
		assertThat(o.getTopic()).isEqualTo(topic);
		assertThat(o.getType()).isEqualTo(type);
		assertThat(o.getExternalReferences()).isEqualTo(externalReferences);
		assertThat(o.getMetadata()).isEqualTo(metadata);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Conversation.create()).hasAllNullFieldsOrProperties();
		assertThat(new Conversation()).hasAllNullFieldsOrProperties();
	}
}
