package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.IN_REPLY_TO;

import java.util.List;
import java.util.UUID;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

class CommunicationEmailHeaderEntityTest {


	@Test
	void testBean() {
		MatcherAssert.assertThat(CommunicationEmailHeaderEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilder() {

		// Arrange
		final var id = UUID.randomUUID().toString();
		final var header = IN_REPLY_TO;
		final var values = List.of("someValue", "someOtherValue");

		// Act
		final var result = CommunicationEmailHeaderEntity.create()
			.withId(id)
			.withHeader(header)
			.withValues(values);

		// Assert
		assertThat(result).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getHeader()).isEqualTo(header);
		assertThat(result.getValues()).isEqualTo(values);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(new CommunicationEmailHeaderEntity()).hasAllNullFieldsOrProperties();
		assertThat(CommunicationEmailHeaderEntity.create()).hasAllNullFieldsOrProperties();
	}

}
