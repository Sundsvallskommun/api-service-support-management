package se.sundsvall.supportmanagement.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.config.action.Config;
import se.sundsvall.supportmanagement.api.model.config.action.Parameter;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;

class ErrandActionMapperTest {

	@Test
	void toActionConfig() {
		final var entity = ActionConfigEntity.create()
			.withId("id")
			.withActive(true)
			.withName("ACTION_NAME")
			.withDisplayValue("Display value");

		final var conditionEntity = ActionConfigConditionEntity.create()
			.withKey("conditionKey")
			.withValues(List.of("condValue1", "condValue2"));
		entity.setConditions(List.of(conditionEntity));

		final var parameterEntity = ActionConfigParameterEntity.create()
			.withKey("paramKey")
			.withValues(List.of("paramValue1"));
		entity.setParameters(List.of(parameterEntity));

		final var result = ErrandActionMapper.toActionConfig(entity);

		assertThat(result.getId()).isEqualTo("id");
		assertThat(result.getActive()).isTrue();
		assertThat(result.getName()).isEqualTo("ACTION_NAME");
		assertThat(result.getDisplayValue()).isEqualTo("Display value");
		assertThat(result.getConditions()).hasSize(1);
		assertThat(result.getConditions().getFirst().getKey()).isEqualTo("conditionKey");
		assertThat(result.getConditions().getFirst().getValues()).containsExactly("condValue1", "condValue2");
		assertThat(result.getParameters()).hasSize(1);
		assertThat(result.getParameters().getFirst().getKey()).isEqualTo("paramKey");
		assertThat(result.getParameters().getFirst().getValues()).containsExactly("paramValue1");
	}

	@Test
	void toParameter() {
		final var conditionEntity = ActionConfigConditionEntity.create()
			.withKey("theKey")
			.withValues(List.of("val1", "val2"));

		final var result = ErrandActionMapper.toParameter(conditionEntity);

		assertThat(result.getKey()).isEqualTo("theKey");
		assertThat(result.getValues()).containsExactly("val1", "val2");
	}

	@Test
	void toEntity() {
		final var config = Config.create()
			.withActive(true)
			.withName("ACTION_NAME")
			.withDisplayValue("Display value")
			.withConditions(List.of(Parameter.create().withKey("condKey").withValues(List.of("condVal"))))
			.withParameters(List.of(Parameter.create().withKey("paramKey").withValues(List.of("paramVal"))));

		final var result = ErrandActionMapper.toEntity("2281", "NAMESPACE", config);

		assertThat(result.getMunicipalityId()).isEqualTo("2281");
		assertThat(result.getNamespace()).isEqualTo("NAMESPACE");
		assertThat(result.getActive()).isTrue();
		assertThat(result.getName()).isEqualTo("ACTION_NAME");
		assertThat(result.getDisplayValue()).isEqualTo("Display value");
		assertThat(result.getConditions()).hasSize(1);
		assertThat(result.getConditions().getFirst().getKey()).isEqualTo("condKey");
		assertThat(result.getConditions().getFirst().getValues()).containsExactly("condVal");
		assertThat(result.getConditions().getFirst().getActionConfigEntity()).isSameAs(result);
		assertThat(result.getParameters()).hasSize(1);
		assertThat(result.getParameters().getFirst().getKey()).isEqualTo("paramKey");
		assertThat(result.getParameters().getFirst().getValues()).containsExactly("paramVal");
		assertThat(result.getParameters().getFirst().getActionConfigEntity()).isSameAs(result);
	}

	@Test
	void testUpdateEntity() {
		final var entity = ActionConfigEntity.create()
			.withId("existingId")
			.withMunicipalityId("2281")
			.withNamespace("NS")
			.withActive(false)
			.withName("OLD_NAME")
			.withDisplayValue("Old display");

		entity.setConditions(new ArrayList<>(List.of(ActionConfigConditionEntity.create()
			.withKey("oldCondKey")
			.withValues(List.of("oldCondVal"))
			.withActionConfigEntity(entity))));

		entity.setParameters(new ArrayList<>(List.of(ActionConfigParameterEntity.create()
			.withKey("oldParamKey")
			.withValues(List.of("oldParamVal"))
			.withActionConfigEntity(entity))));

		final var config = Config.create()
			.withActive(true)
			.withName("NEW_NAME")
			.withDisplayValue("New display")
			.withConditions(List.of(Parameter.create().withKey("newCondKey").withValues(List.of("newCondVal"))))
			.withParameters(List.of(Parameter.create().withKey("newParamKey").withValues(List.of("newParamVal"))));

		final var result = ErrandActionMapper.updateEntity(entity, config);

		assertThat(result).isSameAs(entity);
		assertThat(result.getId()).isEqualTo("existingId");
		assertThat(result.getMunicipalityId()).isEqualTo("2281");
		assertThat(result.getNamespace()).isEqualTo("NS");
		assertThat(result.getActive()).isTrue();
		assertThat(result.getName()).isEqualTo("NEW_NAME");
		assertThat(result.getDisplayValue()).isEqualTo("New display");
		assertThat(result.getConditions()).hasSize(1);
		assertThat(result.getConditions().getFirst().getKey()).isEqualTo("newCondKey");
		assertThat(result.getConditions().getFirst().getValues()).containsExactly("newCondVal");
		assertThat(result.getConditions().getFirst().getActionConfigEntity()).isSameAs(entity);
		assertThat(result.getParameters()).hasSize(1);
		assertThat(result.getParameters().getFirst().getKey()).isEqualTo("newParamKey");
		assertThat(result.getParameters().getFirst().getValues()).containsExactly("newParamVal");
		assertThat(result.getParameters().getFirst().getActionConfigEntity()).isSameAs(entity);
	}

	@Test
	void testToMap() {
		final var parameters = List.of(
			Parameter.create().withKey("key1").withValues(List.of("val1a", "val1b")),
			Parameter.create().withKey("key2").withValues(List.of("val2a")));

		final var result = ErrandActionMapper.toMap(parameters);

		assertThat(result).hasSize(2);
		assertThat(result.get("key1")).containsExactly("val1a", "val1b");
		assertThat(result.get("key2")).containsExactly("val2a");
	}
}
