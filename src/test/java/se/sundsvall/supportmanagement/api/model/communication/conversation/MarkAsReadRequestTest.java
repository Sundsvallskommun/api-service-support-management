package se.sundsvall.supportmanagement.api.model.communication.conversation;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class MarkAsReadRequestTest {

	@Test
	void testBean() {
		assertThat(MarkAsReadRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var messageIds = List.of("id1", "id2");

		final var o = MarkAsReadRequest.create()
			.withMessageIds(messageIds);

		assertThat(o).isNotNull();
		assertThat(o.getMessageIds()).isEqualTo(messageIds);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MarkAsReadRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new MarkAsReadRequest()).hasAllNullFieldsOrProperties();
	}
}
