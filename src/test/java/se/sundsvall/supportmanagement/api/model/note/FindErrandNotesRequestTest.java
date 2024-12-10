package se.sundsvall.supportmanagement.api.model.note;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class FindErrandNotesRequestTest {

	@Test
	void testBean() {
		assertThat(FindErrandNotesRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var context = "context";
		final var limit = 1;
		final var page = 2;
		final var partyId = UUID.randomUUID().toString();
		final var role = "role";

		final var findErrandNotesRequest = FindErrandNotesRequest.create()
			.withContext(context)
			.withLimit(limit)
			.withPage(page)
			.withPartyId(partyId)
			.withRole(role);

		assertThat(findErrandNotesRequest).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(findErrandNotesRequest.getContext()).isEqualTo(context);
		assertThat(findErrandNotesRequest.getLimit()).isEqualTo(limit);
		assertThat(findErrandNotesRequest.getPage()).isEqualTo(page);
		assertThat(findErrandNotesRequest.getPartyId()).isEqualTo(partyId);
		assertThat(findErrandNotesRequest.getRole()).isEqualTo(role);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FindErrandNotesRequest.create()).hasAllNullFieldsOrPropertiesExcept("limit", "page");
		assertThat(FindErrandNotesRequest.create().getLimit()).isEqualTo(100);
		assertThat(FindErrandNotesRequest.create().getPage()).isEqualTo(1);
	}
}
