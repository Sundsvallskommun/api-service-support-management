package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;

class MetadataMapperTest {

	@Test
	void toCategory() {
		final var categoryCreated = OffsetDateTime.now().minusDays(1);
		final var categoryDisplayName = "categoryDisplayName";
		final var categoryModified = OffsetDateTime.now();
		final var categoryName = "categoryName";
		final var typeCreated = OffsetDateTime.now().minusDays(3);
		final var typeDisplayName = "typeDisplayName";
		final var typeEscalationEmail = "typeEscalationEmail";
		final var typeModified = OffsetDateTime.now().minusDays(2);
		final var typeName = "typeName";
		final var entity = createCategoryEntity(categoryCreated, categoryDisplayName, categoryModified, categoryName, typeCreated, typeDisplayName, typeEscalationEmail, typeModified, typeName);

		final var bean = MetadataMapper.toCategory(entity);

		assertThat(bean.getCreated()).isEqualTo(categoryCreated);
		assertThat(bean.getDisplayName()).isEqualTo(categoryDisplayName);
		assertThat(bean.getModified()).isEqualTo(categoryModified);
		assertThat(bean.getName()).isEqualTo(categoryName);
		assertThat(bean.getTypes()).hasSize(1)
			.extracting(
				Type::getCreated,
				Type::getDisplayName,
				Type::getEscalationEmail,
				Type::getModified,
				Type::getName)
			.containsExactly(
				tuple(typeCreated, typeDisplayName, typeEscalationEmail, typeModified, typeName));
	}

	@Test
	void toCategoryForEmptyEntity() {
		assertThat(MetadataMapper.toCategory(CategoryEntity.create()))
			.hasAllNullFieldsOrPropertiesExcept("types")
			.hasFieldOrPropertyWithValue("types", emptyList());;
	}

	@Test
	void toCategoryForNull() {
		assertThat(MetadataMapper.toCategory(null)).isNull();
	}

	@Test
	void toExternalIdType() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var name = "categoryName";

		final var entity = ExternalIdTypeEntity.create()
			.withCreated(created)
			.withModified(modified)
			.withName(name);

		final var bean = MetadataMapper.toExternalIdType(entity);

		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getModified()).isEqualTo(modified);
		assertThat(bean.getName()).isEqualTo(name);
	}

	@Test
	void toExternalIdTypeForEmptyEntity() {
		assertThat(MetadataMapper.toExternalIdType(ExternalIdTypeEntity.create())).hasAllNullFieldsOrProperties();
	}

	@Test
	void toExternalIdTypeForNull() {
		assertThat(MetadataMapper.toExternalIdType(null)).isNull();
	}

	@Test
	void toStatus() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var name = "categoryName";

		final var entity = StatusEntity.create()
			.withCreated(created)
			.withModified(modified)
			.withName(name);

		final var bean = MetadataMapper.toStatus(entity);

		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getModified()).isEqualTo(modified);
		assertThat(bean.getName()).isEqualTo(name);
	}

	@Test
	void toStatusForEmptyEntity() {
		assertThat(MetadataMapper.toStatus(StatusEntity.create())).hasAllNullFieldsOrProperties();
	}

	@Test
	void toStatusForNull() {
		assertThat(MetadataMapper.toStatus(null)).isNull();
	}

	@ParameterizedTest
	@MethodSource(value = "toStatusEntityArguments")
	void toStatusEntity(String namespace, String municipalityId, Status status, StatusEntity expectedResult) {
		assertThat(MetadataMapper.toStatusEntity(namespace, municipalityId, status)).isEqualTo(expectedResult);
	}

	private static Stream<Arguments> toStatusEntityArguments() {
		return Stream.of(
			Arguments.of("namespace", "municipalityId", null, null),
			Arguments.of("namespace", null, Status.create().withName("name"), null),
			Arguments.of(null, "municipalityId", Status.create().withName("name"), null),
			Arguments.of("namespace", "municipalityId", Status.create().withName("name"), StatusEntity.create().withNamespace("namespace").withMunicipalityId("municipalityId").withName("name")));
	}

	private static CategoryEntity createCategoryEntity(OffsetDateTime categoryCreated, String categoryDisplayName, OffsetDateTime categoryModified, String categoryName,
		OffsetDateTime typeCreated, String typeDisplayName, String typeEscalationEmail, OffsetDateTime typeModified, String typeName) {
		return CategoryEntity.create()
			.withCreated(categoryCreated)
			.withDisplayName(categoryDisplayName)
			.withModified(categoryModified)
			.withName(categoryName)
			.withTypes(List.of(createTypeEntity(typeCreated, typeDisplayName, typeEscalationEmail, typeModified, typeName)));
	}

	private static TypeEntity createTypeEntity(OffsetDateTime typeCreated, String typeDisplayName, String typeEscalationEmail, OffsetDateTime typeModified, String typeName) {
		return TypeEntity.create()
			.withCreated(typeCreated)
			.withDisplayName(typeDisplayName)
			.withEscalationEmail(typeEscalationEmail)
			.withModified(typeModified)
			.withName(typeName);
	}
}
