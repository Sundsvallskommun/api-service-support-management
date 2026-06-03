package se.sundsvall.supportmanagement.api.model.handover;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class DirectlyCopyableTest {

	@Test
	void testBean() {
		assertThat(DirectlyCopyable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var title = "Trasig dörr på Storgatan";
		final var priority = "HIGH";
		final var stakeholderCount = 3;
		final var externalTagCount = 5;
		final var attachmentCount = 2;

		final var bean = DirectlyCopyable.create()
			.withTitle(title)
			.withPriority(priority)
			.withStakeholderCount(stakeholderCount)
			.withExternalTagCount(externalTagCount)
			.withAttachmentCount(attachmentCount);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getTitle()).isEqualTo(title);
		assertThat(bean.getPriority()).isEqualTo(priority);
		assertThat(bean.getStakeholderCount()).isEqualTo(stakeholderCount);
		assertThat(bean.getExternalTagCount()).isEqualTo(externalTagCount);
		assertThat(bean.getAttachmentCount()).isEqualTo(attachmentCount);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DirectlyCopyable.create()).hasAllNullFieldsOrProperties();
		assertThat(new DirectlyCopyable()).hasAllNullFieldsOrProperties();
	}
}
