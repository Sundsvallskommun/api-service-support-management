package se.sundsvall.supportmanagement.service.util;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.communication.ConversationEntity;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class ConversationEventTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(ConversationEvent.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {
		// Arrange
		final var conversationEntity = ConversationEntity.create();
		final var requestId = "request-id";
		// Act
		final var result = ConversationEvent.create()
			.withConversationEntity(conversationEntity)
			.withRequestId(requestId);

		// Assert
		assertThat(result).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(result.getConversationEntity()).isEqualTo(conversationEntity);
		assertThat(result.getRequestId()).isEqualTo(requestId);

	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ConversationEvent.create()).hasAllNullFieldsOrProperties();
		assertThat(new ConversationEvent()).hasAllNullFieldsOrProperties();
	}

}
