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

	@InjectMocks
	private MeasureValidator validator;

	@Test
	void acceptsKnownType() {

		// Arrange
		final var measure = Measure.create().withMeasureTypeId("dd000000-0000-0000-0000-000000000100");
		when(measureTypeRepositoryMock.existsByIdAndNamespaceAndMunicipalityId("dd000000-0000-0000-0000-000000000100", NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		// Act & Assert
		assertThatCode(() -> validator.validate(measure, NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
	}

	@Test
	void rejectsUnknownType() {

		// Arrange
		final var measure = Measure.create().withMeasureTypeId("00000000-0000-0000-0000-000000000000");
		when(measureTypeRepositoryMock.existsByIdAndNamespaceAndMunicipalityId("00000000-0000-0000-0000-000000000000", NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		// Act & Assert
		assertThatThrownBy(() -> validator.validate(measure, NAMESPACE, MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasMessage("Bad Request: '00000000-0000-0000-0000-000000000000' is not a valid measure type id for namespace 'namespace' and municipality with id '2281'");
	}

	/**
	 * A patch says nothing about the fields it leaves out, so a null is not something to reject here.
	 */
	@Test
	void skipsAbsentFields() {

		// Act & Assert
		assertThatCode(() -> validator.validate(Measure.create(), NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
		verifyNoInteractions(measureTypeRepositoryMock);
	}

	@Test
	void acceptsNothingToValidate() {

		// Act & Assert
		assertThatCode(() -> validator.validate((List<Measure>) null, NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
		assertThatCode(() -> validator.validate((Measure) null, NAMESPACE, MUNICIPALITY_ID)).doesNotThrowAnyException();
		verifyNoInteractions(measureTypeRepositoryMock);
	}
}
