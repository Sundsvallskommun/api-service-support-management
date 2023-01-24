package se.sundsvall.supportmanagement.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class MetaDataTest {

	@Test
	void testBean() {
		assertThat(MetaData.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var count = 13;
		final var limit = 24;
		final var page = 35;
		final var totalPages = 46;
		final var totalRecords = 57;

		final var bean = MetaData.create()
			.withCount(count)
			.withLimit(limit)
			.withPage(page)
			.withTotalPages(totalPages)
			.withTotalRecords(totalRecords);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getCount()).isEqualTo(count);
		assertThat(bean.getLimit()).isEqualTo(limit);
		assertThat(bean.getPage()).isEqualTo(page);
		assertThat(bean.getTotalPages()).isEqualTo(totalPages);
		assertThat(bean.getTotalRecords()).isEqualTo(totalRecords);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MetaData.create().getCount()).isZero();
		assertThat(MetaData.create().getLimit()).isZero();
		assertThat(MetaData.create().getPage()).isZero();
		assertThat(MetaData.create().getTotalPages()).isZero();
		assertThat(MetaData.create().getTotalRecords()).isZero();
	}
}
