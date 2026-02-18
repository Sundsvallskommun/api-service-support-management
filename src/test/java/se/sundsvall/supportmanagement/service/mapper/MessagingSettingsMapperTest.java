package se.sundsvall.supportmanagement.service.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingSettingsMapperTest {

	@Test
	void toFilterSTring() {
		final var namespace = "my-namespace";
		final var departmentName = "my-department-name";

		final var result = MessagingSettingsMapper.toFilterString(namespace, departmentName);

		assertThat(result).isEqualTo("exists(values.key: 'namespace' and values.value: 'my-namespace') and exists(values.key: 'department_name' and values.value: 'my-department-name')");
	}

}
