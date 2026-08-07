package se.sundsvall.supportmanagement.service.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.config.Validation;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.CATEGORY;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.STATUS;

class ValidationMapperTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	private final ValidationMapper mapper = new ValidationMapper();

	@Test
	void toValidationsReturnsAllTypesWithStoredValues() {
		final var created = now().minusDays(1);
		final var modified = now();
		final var entities = List.of(ValidationEntity.create()
			.withType(STATUS)
			.withValidated(true)
			.withCreated(created)
			.withModified(modified));

		final var validations = mapper.toValidations(entities);

		assertThat(validations)
			.hasSize(EntityType.values().length)
			.extracting(Validation::getType, Validation::getValidated)
			.contains(tuple(STATUS, true), tuple(CATEGORY, false));

		assertThat(validations).filteredOn(validation -> validation.getType() == STATUS)
			.extracting(Validation::getCreated, Validation::getModified)
			.containsExactly(tuple(created, modified));
	}

	@Test
	void toValidationsWithNullReturnsAllTypesAsNotValidated() {
		final var validations = mapper.toValidations(null);

		assertThat(validations)
			.hasSize(EntityType.values().length)
			.extracting(Validation::getValidated, Validation::getCreated, Validation::getModified)
			.containsOnly(tuple(false, null, null));
	}

	@Test
	void toValidation() {
		final var created = now().minusDays(1);
		final var entity = ValidationEntity.create()
			.withType(CATEGORY)
			.withValidated(true)
			.withCreated(created);

		final var validation = mapper.toValidation(entity);

		assertThat(validation.getType()).isEqualTo(CATEGORY);
		assertThat(validation.getValidated()).isTrue();
		assertThat(validation.getCreated()).isEqualTo(created);
		assertThat(validation.getModified()).isNull();
	}

	@Test
	void toEntity() {
		final var entity = mapper.toEntity(NAMESPACE, MUNICIPALITY_ID, STATUS, true);

		assertThat(entity.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(entity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(entity.getType()).isEqualTo(STATUS);
		assertThat(entity.isValidated()).isTrue();
		assertThat(entity.getId()).isNull();
	}

	@Test
	void toValidationsHandlesOffsetDateTimeAsNull() {
		final var entities = List.<ValidationEntity>of();

		final var validations = mapper.toValidations(entities);

		assertThat(validations).extracting(Validation::getCreated).containsOnly((OffsetDateTime) null);
	}
}
