package se.sundsvall.supportmanagement.integration.db.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ErrandNumberSequenceRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandNumberSequenceEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ValueType.STRING;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SHORT_CODE;

@ExtendWith(MockitoExtension.class)
class ErrandNumberGeneratorServiceTest {

	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyMM");
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "CONTACTCENTER";
	private static final String SHORT_CODE = "KC";

	@Mock
	private ErrandNumberSequenceRepository repositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@InjectMocks
	private ErrandNumberGeneratorService stringGeneratorService;

	@Test
	void generateErrandNumber_resetSequence() {

		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(NamespaceConfigEntity.create().withValue(
			NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_SHORT_CODE).withType(STRING).withValue(SHORT_CODE))));
		when(repositoryMock.findByNamespaceAndMunicipalityId(any(String.class), any(String.class))).thenReturn(Optional.of(new ErrandNumberSequenceEntity()
			.withNamespace(NAMESPACE)
			.withNamespace(MUNICIPALITY_ID)
			.withLastSequenceNumber(1234)
			.withResetYearMonth(dateFormatter.format(LocalDate.now().minusMonths(2)))));

		final var result = stringGeneratorService.generateErrandNumber(NAMESPACE, MUNICIPALITY_ID);

		assertThat(result).isEqualTo(String.format("%s-%s0001", SHORT_CODE, dateFormatter.format(LocalDate.now())));
	}

	@Test
	void generateErrandNumber_incrementSequence() {

		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(NamespaceConfigEntity.create().withValue(
			NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_SHORT_CODE).withType(STRING).withValue(SHORT_CODE))));
		when(repositoryMock.findByNamespaceAndMunicipalityId(any(String.class), any(String.class))).thenReturn(Optional.of(new ErrandNumberSequenceEntity()
			.withNamespace(NAMESPACE)
			.withNamespace(MUNICIPALITY_ID)
			.withLastSequenceNumber(123)
			.withResetYearMonth(dateFormatter.format(LocalDate.now()))));

		final var result = stringGeneratorService.generateErrandNumber(NAMESPACE, MUNICIPALITY_ID);

		assertThat(result).isEqualTo(String.format("%s-%s0124", SHORT_CODE, dateFormatter.format(LocalDate.now())));
	}

	@Test
	void generateErrandNumber_noSequence() {

		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(NamespaceConfigEntity.create().withValue(
			NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_SHORT_CODE).withType(STRING).withValue(SHORT_CODE))));
		when(repositoryMock.findByNamespaceAndMunicipalityId(any(String.class), any(String.class))).thenReturn(Optional.empty());

		final var result = stringGeneratorService.generateErrandNumber(NAMESPACE, MUNICIPALITY_ID);

		assertThat(result).isEqualTo(String.format("%s-%s0001", SHORT_CODE, dateFormatter.format(LocalDate.now())));
	}

	@Test
	void generateErrandNumber_unknownNamespace() {

		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.empty());
		assertThatThrownBy(() -> stringGeneratorService.generateErrandNumber("OTHER_NAMESPACE", MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasMessage(String.format("Internal Server Error: Missing shortCode for namespace/municipalityId: 'OTHER_NAMESPACE/%s'. Add via /namespaceConfig resource.", MUNICIPALITY_ID));
	}

	@Test
	void generateErrandNumber_unknownMunicipalityId() {

		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.empty());
		assertThatThrownBy(() -> stringGeneratorService.generateErrandNumber(NAMESPACE, "OTHER_MUNICIPALITY_ID"))
			.isInstanceOf(Problem.class)
			.hasMessage(String.format("Internal Server Error: Missing shortCode for namespace/municipalityId: '%s/OTHER_MUNICIPALITY_ID'. Add via /namespaceConfig resource.", NAMESPACE));
	}

	@Test
	void generateErrandNumber_alwaysUnique() {
		final var entity = new ErrandNumberSequenceEntity()
			.withNamespace(NAMESPACE)
			.withNamespace(MUNICIPALITY_ID)
			.withLastSequenceNumber(1234)
			.withResetYearMonth(dateFormatter.format(LocalDate.now().minusMonths(2)));
		final var maxCount = 99999;

		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(NamespaceConfigEntity.create().withValue(
			NamespaceConfigValueEmbeddable.create().withKey(PROPERTY_SHORT_CODE).withType(STRING).withValue(SHORT_CODE))));
		when(repositoryMock.findByNamespaceAndMunicipalityId(any(String.class), any(String.class))).thenReturn(Optional.of(entity));

		final var result = IntStream.range(0, maxCount).mapToObj(_ -> stringGeneratorService.generateErrandNumber(NAMESPACE, MUNICIPALITY_ID))
			.toList();

		assertThat(result).hasSize(maxCount).doesNotHaveDuplicates();
		assertThat(result.getLast()).endsWith("99999");
	}
}
