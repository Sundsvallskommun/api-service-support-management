package se.sundsvall.supportmanagement.api.model.errand;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class JsonParameterTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OBJECT_MAPPER.createObjectNode().put("field", new Random().nextInt()), JsonNode.class);
	}

	@Test
	void testBean() {
		assertThat(JsonParameter.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var key = "formData";
		final var value = OBJECT_MAPPER.createObjectNode().put("field", "value");
		final var schemaId = "550e8400-e29b-41d4-a716-446655440000";

		final var bean = JsonParameter.create()
			.withKey(key)
			.withValue(value)
			.withSchemaId(schemaId);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getKey()).isEqualTo(key);
		assertThat(bean.getValue()).isEqualTo(value);
		assertThat(bean.getSchemaId()).isEqualTo(schemaId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(JsonParameter.create()).hasAllNullFieldsOrProperties();
		assertThat(new JsonParameter()).hasAllNullFieldsOrProperties();
	}

}
