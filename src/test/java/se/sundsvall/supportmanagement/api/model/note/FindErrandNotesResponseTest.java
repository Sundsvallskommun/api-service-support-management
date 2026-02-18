package se.sundsvall.supportmanagement.api.model.note;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.MetaData;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class FindErrandNotesResponseTest {

	@Test
	void testBean() {
		assertThat(FindErrandNotesResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var metaData = MetaData.create();
		final var notes = List.of(ErrandNote.create());

		final var findErrandNotesResponse = FindErrandNotesResponse.create()
			.withMetaData(metaData)
			.withNotes(notes);

		assertThat(findErrandNotesResponse).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(findErrandNotesResponse.getMetaData()).isEqualTo(metaData);
		assertThat(findErrandNotesResponse.getNotes()).isEqualTo(notes);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FindErrandNotesResponse.create()).hasAllNullFieldsOrProperties();
		assertThat(new FindErrandNotesResponse()).hasAllNullFieldsOrProperties();
	}
}
