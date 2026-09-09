package se.sundsvall.supportmanagement.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureTypeEntity;
import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@ExtendWith(MockitoExtension.class)
class MeasureValidatorTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String TYPE_ID = "dd000000-0000-0000-0000-000000000100";
	private static final String ROLE_ID = "cc000000-0000-0000-0000-000000000100";

	@Mock
	private MeasureTypeRepository measureTypeRepository;
	@Mock
	private RoleRepository roleRepository;
	@Mock
	private AccessControlService accessControlService;
	@InjectMocks
	private MeasureValidator validator;

	private MeasureTypeEntity activeType() {
		final var type = MeasureTypeEntity.create().withId(TYPE_ID).withMeasureGroup("INDEPENDENT_GROUP");
		when(measureTypeRepository.findByIdAndNamespaceAndMunicipalityId(TYPE_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(type));
		return type;
	}

	private RoleEntity activeRole() {
		final var role = RoleEntity.create().withId(ROLE_ID).withName("MANAGER");
		when(roleRepository.findByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "MANAGER"))
			.thenReturn(Optional.of(role));
		return role;
	}

	private Measure registration() {
		return Measure.create().withMeasureTypeId(TYPE_ID).withAddedByRole("MANAGER");
	}

	@Test
	void acceptsAnyActiveTypeForAHeldRoleAndSetsTheVerifiedCreator() {
		activeType();
		activeRole();
		when(accessControlService.verifyMeasureCreator(NAMESPACE, MUNICIPALITY_ID, null, "MANAGER")).thenReturn("joe01doe");
		final var measure = registration();
		validator.validate(measure, NAMESPACE, MUNICIPALITY_ID);
		assertThat(measure.getAddedByUser()).isEqualTo("joe01doe");
	}

	@Test
	void rejectsDeprecatedTypes() {
		activeType().withDeprecated(true);
		assertThatThrownBy(() -> validator.validate(registration(), NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("active measure type");
	}

	@Test
	void rejectsDeprecatedRegistrationRoles() {
		activeType();
		activeRole().withDeprecated(true);
		assertThatThrownBy(() -> validator.validate(registration(), NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("active registration role");
		verifyNoInteractions(accessControlService);
	}

	@Test
	void rejectsUnknownOrForeignNamespaceTypes() {
		assertThatThrownBy(() -> validator.validate(registration(), NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("does not exist in this municipality and namespace");
		verifyNoInteractions(roleRepository, accessControlService);
	}

	@Test
	void rejectsUsersWithoutTheSelectedRole() {
		activeType();
		activeRole();
		when(accessControlService.verifyMeasureCreator(NAMESPACE, MUNICIPALITY_ID, null, "MANAGER")).thenThrow(Problem.valueOf(FORBIDDEN, "Role not granted"));
		assertThatThrownBy(() -> validator.validate(registration(), NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("Role not granted");
	}

	@Test
	void preservesHistoryWhenTypeOrRoleHaveBeenRetired() {
		final var existing = MeasureEntity.create().withMeasureTypeId(TYPE_ID).withAddedByRole("RETIRED").withAddedByUser("original");
		assertThatCode(() -> validator.validateUpdate(Measure.create().withGoal("Revised").withMeasureTypeId(TYPE_ID), existing, NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
		verifyNoInteractions(measureTypeRepository, roleRepository, accessControlService);
	}

	@Test
	void rejectsCreatorOrRegistrationRoleChanges() {
		final var existing = MeasureEntity.create().withAddedByRole("MANAGER").withAddedByUser("original");
		assertThatThrownBy(() -> validator.validateUpdate(Measure.create().withAddedByUser("other"), existing, NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("cannot be changed");
		assertThatThrownBy(() -> validator.validateUpdate(Measure.create().withAddedByRole("OTHER"), existing, NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("cannot be changed");
	}

	@Test
	void typeChangesDoNotReassignOrReauthorizeTheHistoricalRegistrationRole() {
		activeType();
		final var existing = MeasureEntity.create().withMeasureTypeId("old-type").withAddedByRole("MANAGER");
		assertThatCode(() -> validator.validateUpdate(Measure.create().withMeasureTypeId(TYPE_ID), existing, NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
		verifyNoInteractions(accessControlService);
	}

	@Test
	void embeddedUpdatesApplyTheSameCreatorAndTypeRules() {
		final var existing = MeasureEntity.create().withId("saved").withAddedByRole("MANAGER").withAddedByUser("original");
		final var patch = Measure.create().withId("saved").withAddedByUser("other");
		assertThatThrownBy(() -> validator.validateUpdate(List.of(patch), List.of(existing), NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("cannot be changed");
	}

	@Test
	void rejectsForeignAndDuplicateMeasureIdsInEmbeddedUpdates() {
		final var existing = MeasureEntity.create().withId("saved");
		assertThatThrownBy(() -> validator.validateUpdate(List.of(Measure.create().withId("other")), List.of(existing), NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("does not belong");
		assertThatThrownBy(() -> validator.validateUpdate(List.of(Measure.create().withId("saved"), Measure.create().withId("saved")), List.of(existing), NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("only occur once");
	}

	@Test
	void checksTheEffectiveDateRangeOnPartialUpdates() {
		final var existing = MeasureEntity.create().withPlannedStart(OffsetDateTime.parse("2026-09-10T12:00:00Z"));
		final var patch = Measure.create().withPlannedComplete(OffsetDateTime.parse("2026-09-09T12:00:00Z"));
		assertThatThrownBy(() -> validator.validateUpdate(patch, existing, NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("must not precede");
	}
}
