package se.sundsvall.supportmanagement.api.model.note;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class CreateErrandNoteRequestTest {

	@Test
	void testBean() {
		assertThat(CreateErrandNoteRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var body = "body";
		final var context = "context";
		final var createdBy = "createdBy";
		final var partyId = randomUUID().toString();
		final var role = "role";
		final var subject = "subject";

		final var createNoteRequest = CreateErrandNoteRequest.create()
			.withBody(body)
			.withCreatedBy(createdBy)
			.withContext(context)
			.withPartyId(partyId)
			.withRole(role)
			.withSubject(subject);

		assertThat(createNoteRequest).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(createNoteRequest.getBody()).isEqualTo(body);
		assertThat(createNoteRequest.getCreatedBy()).isEqualTo(createdBy);
		assertThat(createNoteRequest.getContext()).isEqualTo(context);
		assertThat(createNoteRequest.getPartyId()).isEqualTo(partyId);
		assertThat(createNoteRequest.getRole()).isEqualTo(role);
		assertThat(createNoteRequest.getSubject()).isEqualTo(subject);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(new CreateErrandNoteRequest()).hasAllNullFieldsOrProperties();
		assertThat(CreateErrandNoteRequest.create()).hasAllNullFieldsOrProperties();
	}
}
