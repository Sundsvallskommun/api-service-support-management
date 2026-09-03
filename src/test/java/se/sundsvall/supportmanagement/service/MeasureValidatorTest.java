package se.sundsvall.supportmanagement.service;

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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasureValidatorTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private MeasureTypeRepository measureTypeRepositoryMock;

	@Mock
	private RoleRepository roleRepositoryMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@InjectMocks
	private MeasureValidator validator;

	@Test
	void acceptsKnownTypeAndRole() {

		// Arrange
		final var measure = Measure.create().withType("INTERVENTION").withAddedByRole("MANAGER");
		when(measureTypeRepositoryMock.findWithLockingByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "INTERVENTION")).thenReturn(Optional.of(MeasureTypeEntity.create().withName("INTERVENTION")));
		when(roleRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "MANAGER")).thenReturn(true);

		// Act & Assert
		assertThatCode(() -> validator.validate(measure, NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
	}

	@Test
	void rejectsUnknownType() {

		// Arrange
		final var measure = Measure.create().withType("NOT_A_TYPE");
		when(measureTypeRepositoryMock.findWithLockingByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "NOT_A_TYPE")).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> validator.validate(measure, NAMESPACE, MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasMessage("Bad Request: 'NOT_A_TYPE' is not a valid measure type for namespace 'namespace' and municipality with id '2281'");
	}

	@Test
	void rejectsUnknownRole() {

		// Arrange
		final var measure = Measure.create().withAddedByRole("NOT_A_ROLE");
		when(roleRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "NOT_A_ROLE")).thenReturn(false);

		// Act & Assert
		assertThatThrownBy(() -> validator.validate(measure, NAMESPACE, MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasMessage("Bad Request: 'NOT_A_ROLE' is not a valid role for namespace 'namespace' and municipality with id '2281'");
	}

	/**
	 * A patch says nothing about the fields it leaves out, so a null is not something to reject here.
	 */
	@Test
	void skipsAbsentFields() {

		// Act & Assert
		assertThatCode(() -> validator.validate(Measure.create(), NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
		verifyNoInteractions(measureTypeRepositoryMock, roleRepositoryMock);
	}

	/**
	 * The list form is what the errand entry point uses, and it must reach the same verdict as the measure resource does
	 * on the same measure - an unknown role included.
	 */
	@Test
	void rejectsUnknownRoleAnywhereInAList() {

		// Arrange
		final var measures = List.of(
			Measure.create().withAddedByRole("MANAGER"),
			Measure.create().withAddedByRole("NOT_A_ROLE"));
		when(roleRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "MANAGER")).thenReturn(true);
		when(roleRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "NOT_A_ROLE")).thenReturn(false);

		// Act & Assert
		assertThatThrownBy(() -> validator.validate(measures, NAMESPACE, MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("not a valid role");
	}

	@Test
	void acceptsNothingToValidate() {

		// Act & Assert
		assertThatCode(() -> validator.validate((List<Measure>) null, NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
		assertThatCode(() -> validator.validate((Measure) null, NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
		verifyNoInteractions(measureTypeRepositoryMock, roleRepositoryMock);
	}

	@Test
	void rejectsDeprecatedTypeForNewMeasures() {
		when(measureTypeRepositoryMock.findWithLockingByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "OLD"))
			.thenReturn(Optional.of(MeasureTypeEntity.create().withName("OLD").withDeprecated(true)));
		assertThatThrownBy(() -> validator.validate(Measure.create().withType("OLD"), NAMESPACE, MUNICIPALITY_ID))
			.hasMessageContaining("deprecated");
	}

	@Test
	void retainsDeprecatedTypeAndOriginalAttribution() {
		final var existing = MeasureEntity.create().withId("id").withType("OLD").withAddedByUser("creator").withAddedByRole("OLD_ROLE");
		final var input = Measure.create().withId("id").withType("OLD").withAddedByUser("creator").withAddedByRole("OLD_ROLE").withGoal("Updated");
		when(measureTypeRepositoryMock.findWithLockingByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "OLD"))
			.thenReturn(Optional.of(MeasureTypeEntity.create().withName("OLD").withDeprecated(true)));
		assertThatCode(() -> validator.validate(List.of(input), List.of(existing), NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
		verifyNoInteractions(roleRepositoryMock, accessControlServiceMock);
	}

	@Test
	void cannotSwitchAnExistingMeasureToADeprecatedType() {
		when(measureTypeRepositoryMock.findWithLockingByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "OLD"))
			.thenReturn(Optional.of(MeasureTypeEntity.create().withName("OLD").withDeprecated(true)));
		assertThatThrownBy(() -> validator.validate(Measure.create().withType("OLD"), MeasureEntity.create().withType("ACTIVE"), NAMESPACE, MUNICIPALITY_ID))
			.hasMessageContaining("deprecated");
	}

	@Test
	void unknownMeasureIdCannotBypassNewTypeValidation() {
		final var input = Measure.create().withId("foreign-id").withType("UNKNOWN");
		assertThatThrownBy(() -> validator.validate(List.of(input), List.of(MeasureEntity.create().withId("id").withType("UNKNOWN")), NAMESPACE, MUNICIPALITY_ID))
			.hasMessageContaining("not a valid measure type");
	}

	@Test
	void cannotRewriteTheCreatorThroughEitherWritePath() {
		final var existing = MeasureEntity.create().withId("id").withAddedByUser("creator").withAddedByRole("MANAGER");
		assertThatThrownBy(() -> validator.validate(Measure.create().withAddedByUser("other"), existing, NAMESPACE, MUNICIPALITY_ID))
			.hasMessageContaining("addedByUser cannot be changed");
		assertThatThrownBy(() -> validator.validate(List.of(Measure.create().withId("id").withAddedByRole("OTHER")), List.of(existing), NAMESPACE, MUNICIPALITY_ID))
			.hasMessageContaining("addedByRole cannot be changed");
	}

	@Test
	void cannotClearAttributionOrType() {
		final var existing = MeasureEntity.create().withType("TYPE").withAddedByUser("creator").withAddedByRole("MANAGER");
		assertThatThrownBy(() -> validator.validate(Measure.create().withType(null), existing, NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("type must not be null");
		assertThatThrownBy(() -> validator.validate(Measure.create().withAddedByUser(null), existing, NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("addedByUser cannot be changed");
		assertThatThrownBy(() -> validator.validate(Measure.create().withAddedByRole(null), existing, NAMESPACE, MUNICIPALITY_ID)).hasMessageContaining("addedByRole cannot be changed");
	}
}
