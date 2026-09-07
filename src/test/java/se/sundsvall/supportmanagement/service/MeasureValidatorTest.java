package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;

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

	@InjectMocks
	private MeasureValidator validator;

	@Test
	void acceptsKnownTypeAndRole() {

		// Arrange
		final var measure = Measure.create().withType("INTERVENTION").withAddedByRole("MANAGER");
		when(measureTypeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "INTERVENTION")).thenReturn(true);
		when(roleRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "MANAGER")).thenReturn(true);

		// Act & Assert
		assertThatCode(() -> validator.validate(measure, NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
	}

	@Test
	void rejectsUnknownType() {

		// Arrange
		final var measure = Measure.create().withType("NOT_A_TYPE");
		when(measureTypeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "NOT_A_TYPE")).thenReturn(false);

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
}
