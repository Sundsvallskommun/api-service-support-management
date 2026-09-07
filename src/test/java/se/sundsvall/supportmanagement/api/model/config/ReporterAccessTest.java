package se.sundsvall.supportmanagement.api.model.config;

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
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField.TITLE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.ERRAND;

class ReporterAccessTest {

	@Test
	void testBean() {
		assertThat(ReporterAccess.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var resources = List.of(ResourceAccess.create().withResource(ERRAND).withLevel(AccessLevel.R));
		final var fields = List.of(FieldAccess.create().withField(TITLE));

		final var bean = ReporterAccess.create()
			.withResources(resources)
			.withFields(fields);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getResources()).isEqualTo(resources);
		assertThat(bean.getFields()).isEqualTo(fields);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ReporterAccess.create()).hasAllNullFieldsOrProperties();
		assertThat(new ReporterAccess()).hasAllNullFieldsOrProperties();
	}
}
