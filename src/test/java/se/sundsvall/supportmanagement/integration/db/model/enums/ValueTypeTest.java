package se.sundsvall.supportmanagement.integration.db.model.enums;

import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;

class ValueTypeTest {

	@Test
	void enumValues() {
		assertThat(ValueType.values()).containsExactlyInAnyOrder(
			ValueType.BOOLEAN,
			ValueType.INTEGER,
			ValueType.STRING);
	}

	@Test
	void enumToString() {
		assertThat(ValueType.BOOLEAN).hasToString("BOOLEAN");
		assertThat(ValueType.INTEGER).hasToString("INTEGER");
		assertThat(ValueType.STRING).hasToString("STRING");
	}

	@Test
	void test() {
		assertThat(ValueType.getAsTypedClass(NamespaceConfigValueEmbeddable.create().withType(ValueType.BOOLEAN).withValue("true"))).isEqualTo(true);
		assertThat(ValueType.getAsTypedClass(NamespaceConfigValueEmbeddable.create().withType(ValueType.INTEGER).withValue("123"))).isEqualTo(123);
		assertThat(ValueType.getAsTypedClass(NamespaceConfigValueEmbeddable.create().withType(ValueType.STRING).withValue("value"))).isEqualTo("value");
	}
}
