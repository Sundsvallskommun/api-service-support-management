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

import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.integration.db.model.CategoryEntity;
import se.sundsvall.supportmanagement.integration.db.model.ExternalIdTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.db.model.TypeEntity;

class MetadataMapperTest {

	// =================================================================
	// Category and Type tests
	// =================================================================

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
			.hasFieldOrPropertyWithValue("types", emptyList());
	}

	@Test
	void toCategoryForNull() {
		assertThat(MetadataMapper.toCategory(null)).isNull();
	}

	// =================================================================
	// ExternalIdType tests
	// =================================================================

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

	@ParameterizedTest
	@MethodSource(value = "toExternalIdTypeEntityArguments")
	void toExternalIdTypeEntity(final String namespace, final String municipalityId, final ExternalIdType externalIdType, final ExternalIdTypeEntity expectedResult) {
		assertThat(MetadataMapper.toExternalIdTypeEntity(namespace, municipalityId, externalIdType)).isEqualTo(expectedResult);
	}

	private static Stream<Arguments> toExternalIdTypeEntityArguments() {
		return Stream.of(
			Arguments.of("namespace", "municipalityId", null, null),
			Arguments.of("namespace", null, ExternalIdType.create().withName("name"), null),
			Arguments.of(null, "municipalityId", ExternalIdType.create().withName("name"), null),
			Arguments.of("namespace", "municipalityId", ExternalIdType.create().withName("name"), ExternalIdTypeEntity.create().withNamespace("namespace").withMunicipalityId("municipalityId").withName("name")));
	}

	// =================================================================
	// Status tests
	// =================================================================

	@Test
	void toStatus() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var name = "statusName";

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
	void toStatusEntity(final String namespace, final String municipalityId, final Status status, final StatusEntity expectedResult) {
		assertThat(MetadataMapper.toStatusEntity(namespace, municipalityId, status)).isEqualTo(expectedResult);
	}

	private static Stream<Arguments> toStatusEntityArguments() {
		return Stream.of(
			Arguments.of("namespace", "municipalityId", null, null),
			Arguments.of("namespace", null, Status.create().withName("name"), null),
			Arguments.of(null, "municipalityId", Status.create().withName("name"), null),
			Arguments.of("namespace", "municipalityId", Status.create().withName("name"), StatusEntity.create().withNamespace("namespace").withMunicipalityId("municipalityId").withName("name")));
	}

	// =================================================================
	// Role tests
	// =================================================================

	@Test
	void toRole() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var name = "roleName";

		final var entity = RoleEntity.create()
			.withCreated(created)
			.withModified(modified)
			.withName(name);

		final var bean = MetadataMapper.toRole(entity);

		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getModified()).isEqualTo(modified);
		assertThat(bean.getName()).isEqualTo(name);
	}

	@Test
	void toRoleForEmptyEntity() {
		assertThat(MetadataMapper.toRole(RoleEntity.create())).hasAllNullFieldsOrProperties();
	}

	@Test
	void toRoleForNull() {
		assertThat(MetadataMapper.toRole(null)).isNull();
	}

	@ParameterizedTest
	@MethodSource(value = "toRoleEntityArguments")
	void toRoleEntity(final String namespace, final String municipalityId, final Role role, final RoleEntity expectedResult) {
		assertThat(MetadataMapper.toRoleEntity(namespace, municipalityId, role)).isEqualTo(expectedResult);
	}

	private static Stream<Arguments> toRoleEntityArguments() {
		return Stream.of(
			Arguments.of("namespace", "municipalityId", null, null),
			Arguments.of("namespace", null, Role.create().withName("name"), null),
			Arguments.of(null, "municipalityId", Role.create().withName("name"), null),
			Arguments.of("namespace", "municipalityId", Role.create().withName("name"), RoleEntity.create().withNamespace("namespace").withMunicipalityId("municipalityId").withName("name")));
	}

	private static CategoryEntity createCategoryEntity(final OffsetDateTime categoryCreated, final String categoryDisplayName, final OffsetDateTime categoryModified, final String categoryName,
		final OffsetDateTime typeCreated, final String typeDisplayName, final String typeEscalationEmail, final OffsetDateTime typeModified, final String typeName) {
		return CategoryEntity.create()
			.withCreated(categoryCreated)
			.withDisplayName(categoryDisplayName)
			.withModified(categoryModified)
			.withName(categoryName)
			.withTypes(List.of(createTypeEntity(typeCreated, typeDisplayName, typeEscalationEmail, typeModified, typeName)));
	}

	private static TypeEntity createTypeEntity(final OffsetDateTime typeCreated, final String typeDisplayName, final String typeEscalationEmail, final OffsetDateTime typeModified, final String typeName) {
		return TypeEntity.create()
			.withCreated(typeCreated)
			.withDisplayName(typeDisplayName)
			.withEscalationEmail(typeEscalationEmail)
			.withModified(typeModified)
			.withName(typeName);
	}
}
