package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.enums.ValueType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod.MANUAL;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SINGLE_DECISION_PER_ERRAND;

@ExtendWith(MockitoExtension.class)
class DecisionValidatorTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";

	@Mock
	private DecisionRepository decisionRepositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@InjectMocks
	private DecisionValidator validator;

	/**
	 * The identifier is bound to the thread, which the test classes run before this one share.
	 */
	@BeforeEach
	@AfterEach
	void clearIdentifier() {
		Identifier.remove();
	}

	private static NamespaceConfigEntity configWithSingleDecision(final boolean value) {
		return NamespaceConfigEntity.create()
			.withValues(List.of(NamespaceConfigValueEmbeddable.create()
				.withKey(PROPERTY_SINGLE_DECISION_PER_ERRAND)
				.withType(ValueType.BOOLEAN)
				.withValue(String.valueOf(value))));
	}

	/**
	 * A namespace that has not asked for the restriction is not restricted, and the errand is then not even asked about.
	 */
	@Test
	void cardinalityIsUncheckedWhenTheNamespaceHasNoConfiguration() {

		// Arrange
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));
		verifyNoInteractions(decisionRepositoryMock);
	}

	@Test
	void cardinalityIsUncheckedWhenTheNamespaceHasNotAskedForIt() {

		// Arrange
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(configWithSingleDecision(false)));

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));
		verifyNoInteractions(decisionRepositoryMock);
	}

	/**
	 * A namespace configured before the setting existed reads as unrestricted rather than failing the request.
	 */
	@Test
	void cardinalityIsUncheckedWhenTheSettingIsAbsentFromTheConfiguration() {

		// Arrange
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(NamespaceConfigEntity.create()));

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));
		verifyNoInteractions(decisionRepositoryMock);
	}

	@Test
	void theFirstDecisionIsAllowedEvenWhenTheNamespaceAllowsOnlyOne() {

		// Arrange
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(configWithSingleDecision(true)));
		when(decisionRepositoryMock.existsByNamespaceAndMunicipalityIdAndErrandEntityId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(false);

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));
	}

	@Test
	void aSecondDecisionIsAConflictWhenTheNamespaceAllowsOnlyOne() {

		// Arrange
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(configWithSingleDecision(true)));
		when(decisionRepositoryMock.existsByNamespaceAndMunicipalityIdAndErrandEntityId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(true);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(problem.getMessage()).contains(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	/**
	 * A patch that says nothing about the method leaves the stored one standing, so there is nothing to check.
	 */
	@Test
	void aMissingMethodIsLeftAlone() {

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateMethod(null));
	}

	@Test
	void anAdAccountMayWriteAManualDecision() {

		// Arrange
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe"));

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateMethod(MANUAL));
	}

	@Test
	void aConsumerMayWriteAnAutomaticDecision() {

		// Arrange
		Identifier.set(Identifier.create().withType(Identifier.Type.PARTY_ID).withValue("81471222-5798-11e9-ae24-57fa13b361e1"));

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateMethod(AUTOMATIC));
	}

	/**
	 * The difference between a decision a person made and one a process made has to be answerable afterwards, which is why
	 * neither side may claim the other.
	 */
	@Test
	void aConsumerCannotWriteAManualDecision() {

		// Arrange
		Identifier.set(Identifier.create().withType(Identifier.Type.PARTY_ID).withValue("81471222-5798-11e9-ae24-57fa13b361e1"));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateMethod(MANUAL));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(problem.getMessage()).contains("MANUAL");
	}

	@Test
	void aManualDecisionWithoutAnIdentifierAtAllIsRejected() {

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateMethod(MANUAL));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(FORBIDDEN);
	}

	@Test
	void anAdAccountCannotWriteAnAutomaticDecision() {

		// Arrange
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe"));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateMethod(AUTOMATIC));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(problem.getMessage()).contains("AUTOMATIC");
	}
}
