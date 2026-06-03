package se.sundsvall.supportmanagement.api.model.errand.handover;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class HandoverIncludeTest {

	@Test
	void testBean() {
		assertThat(HandoverInclude.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var include = HandoverInclude.create()
			.withStakeholders(true)
			.withExternalTags(true)
			.withParameters(true)
			.withJsonParameters(true)
			.withAttachments(true)
			.withBusinessRelated(true)
			.withEscalationEmail(true)
			.withContactReasonDescription(true);

		assertThat(include).isNotNull();
		assertThat(include.isStakeholders()).isTrue();
		assertThat(include.isExternalTags()).isTrue();
		assertThat(include.isParameters()).isTrue();
		assertThat(include.isJsonParameters()).isTrue();
		assertThat(include.isAttachments()).isTrue();
		assertThat(include.isBusinessRelated()).isTrue();
		assertThat(include.isEscalationEmail()).isTrue();
		assertThat(include.isContactReasonDescription()).isTrue();
	}

	@Test
	void testDefaultsOnCreatedBean() {
		final var include = HandoverInclude.create();
		assertThat(include.isStakeholders()).isFalse();
		assertThat(include.isExternalTags()).isFalse();
		assertThat(include.isParameters()).isFalse();
		assertThat(include.isJsonParameters()).isFalse();
		assertThat(include.isAttachments()).isFalse();
		assertThat(include.isBusinessRelated()).isFalse();
		assertThat(include.isEscalationEmail()).isFalse();
		assertThat(include.isContactReasonDescription()).isFalse();
	}
}
