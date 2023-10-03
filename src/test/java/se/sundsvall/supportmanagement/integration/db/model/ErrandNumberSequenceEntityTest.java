package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import org.junit.jupiter.api.Test;

class ErrandNumberSequenceEntityTest {

	@Test
	void testBean() {

		assertThat(ErrandNumberSequenceEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var namespace = "namespace";
		final var municipalityId = "2281";
		final var lastSequenceNumber = 1;
		final var resetYearMonth = "2101";

		final var entity = ErrandNumberSequenceEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withLastSequenceNumber(lastSequenceNumber)
			.withResetYearMonth(resetYearMonth);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getResetYearMonth()).isEqualTo(resetYearMonth);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandNumberSequenceEntity.create()).satisfies(bean -> {
			assertThat(bean.getNamespace()).isNull();
			assertThat(bean.getLastSequenceNumber()).isZero();
			assertThat(bean.getResetYearMonth()).isNull();
			assertThat(bean.getMunicipalityId()).isNull();
		});
		assertThat(new ErrandNumberSequenceEntity()).satisfies(bean -> {
			assertThat(bean.getNamespace()).isNull();
			assertThat(bean.getLastSequenceNumber()).isZero();
			assertThat(bean.getResetYearMonth()).isNull();
			assertThat(bean.getMunicipalityId()).isNull();
		});
	}

}
