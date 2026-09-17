package se.sundsvall.supportmanagement.api.model.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AffectedActionTest {

	@Test
	void constructors() {
		assertThat(new AffectedAction()).hasAllNullFieldsOrProperties();
		assertThat(AffectedAction.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void gettersAndSetters() {
		var bean = new AffectedAction();
		bean.setId("id");
		bean.setName("name");
		bean.setDisplayValue("display");

		assertThat(bean.getId()).isEqualTo("id");
		assertThat(bean.getName()).isEqualTo("name");
		assertThat(bean.getDisplayValue()).isEqualTo("display");
	}

	@Test
	void withers() {
		var bean = AffectedAction.create()
			.withId("id")
			.withName("name")
			.withDisplayValue("display");

		assertThat(bean.getId()).isEqualTo("id");
		assertThat(bean.getName()).isEqualTo("name");
		assertThat(bean.getDisplayValue()).isEqualTo("display");
	}

	@Test
	void equalsAndHashCode() {
		var a = AffectedAction.create().withId("id").withName("name").withDisplayValue("display");
		var b = AffectedAction.create().withId("id").withName("name").withDisplayValue("display");
		var c = AffectedAction.create().withId("other");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
	}

	@Test
	void toStringContainsFields() {
		var bean = AffectedAction.create().withId("id").withName("name").withDisplayValue("display");
		assertThat(bean.toString()).contains("id", "name", "display");
	}
}
