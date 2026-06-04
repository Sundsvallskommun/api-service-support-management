package se.sundsvall.supportmanagement.api.model.errand.handover;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class HandoverSourceHandlingTest {

	@Test
	void testBean() {
		// hasValidBeanEquals/HashCode excluded: BeanMatchers cannot generate two distinct HandoverSourceAction values
		// (single-value enum)
		assertThat(HandoverSourceHandling.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanToString()));
	}

	@Test
	void testEqualsAndHashCode() {
		final var a = HandoverSourceHandling.create()
			.withAction(HandoverSourceAction.CLOSE)
			.withResolution("HANDED_OVER")
			.withClosingComment("comment");
		final var b = HandoverSourceHandling.create()
			.withAction(HandoverSourceAction.CLOSE)
			.withResolution("HANDED_OVER")
			.withClosingComment("comment");

		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(HandoverSourceHandling.create().withResolution("OTHER"));
	}

	@Test
	void testBuilderMethods() {
		final var action = HandoverSourceAction.CLOSE;
		final var resolution = "HANDED_OVER";
		final var closingComment = "Överlämnad till annan drake";

		final var sourceHandling = HandoverSourceHandling.create()
			.withAction(action)
			.withResolution(resolution)
			.withClosingComment(closingComment);

		assertThat(sourceHandling).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(sourceHandling.getAction()).isEqualTo(action);
		assertThat(sourceHandling.getResolution()).isEqualTo(resolution);
		assertThat(sourceHandling.getClosingComment()).isEqualTo(closingComment);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(HandoverSourceHandling.create()).hasAllNullFieldsOrProperties();
		assertThat(new HandoverSourceHandling()).hasAllNullFieldsOrProperties();
	}
}
