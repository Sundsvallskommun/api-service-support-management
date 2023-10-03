package se.sundsvall.supportmanagement.integration.db.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.integration.db.ErrandNumberSequenceRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandNumberSequenceEntity;


@ExtendWith(MockitoExtension.class)
class ErrandNumberGeneratorTest {

	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyMM");

	private static final String MUNICIPALITY_ID = "2281";

	private static final String NAMESPACE = "CONTACTCENTER";
	@Mock
	ErrandNumberSequenceRepository repository;

	@InjectMocks
	ErrandNumberGeneratorService stringGeneratorService;

	@Test
	void generateErrandNumber_resetSequence() {

		when(repository.findByNamespaceAndMunicipalityId(any(String.class), any(String.class))).thenReturn(Optional.of(new ErrandNumberSequenceEntity()
			.withNamespace(NAMESPACE)
			.withNamespace(MUNICIPALITY_ID)
			.withLastSequenceNumber(1234)
			.withResetYearMonth(dateFormatter.format(LocalDate.now().minusMonths(2)))));

		final var result = stringGeneratorService.generateErrandNumber(NAMESPACE, MUNICIPALITY_ID);

		assertThat(result).isEqualTo("KC-" + dateFormatter.format(LocalDate.now()) + "0001");
	}

	@Test
	void generateErrandNumber_incrementSequence() {

		when(repository.findByNamespaceAndMunicipalityId(any(String.class), any(String.class))).thenReturn(Optional.of(new ErrandNumberSequenceEntity()
			.withNamespace(NAMESPACE)
			.withNamespace(MUNICIPALITY_ID)
			.withLastSequenceNumber(123)
			.withResetYearMonth(dateFormatter.format(LocalDate.now()))));

		final var result = stringGeneratorService.generateErrandNumber(NAMESPACE, MUNICIPALITY_ID);

		assertThat(result).isEqualTo("KC-" + dateFormatter.format(LocalDate.now()) + "0124");
	}

	@Test
	void generateErrandNumber_noSequence() {

		when(repository.findByNamespaceAndMunicipalityId(any(String.class), any(String.class))).thenReturn(Optional.empty());

		final var result = stringGeneratorService.generateErrandNumber(NAMESPACE, MUNICIPALITY_ID);

		assertThat(result).isEqualTo("KC-" + dateFormatter.format(LocalDate.now()) + "0001");
	}


	@Test
	void generateErrandNumber_unknownNamespace() {

		assertThatThrownBy(() -> stringGeneratorService.generateErrandNumber("OTHER_NAMESPACE", MUNICIPALITY_ID))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("No namespace shortcode found for namespace: OTHER_NAMESPACE and municipalityId: " + MUNICIPALITY_ID);

	}

	@Test
	void generateErrandNumber_unknownMunicipalityId() {

		assertThatThrownBy(() -> stringGeneratorService.generateErrandNumber(NAMESPACE, "OTHER_MUNICIPALITY_ID"))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("No namespace shortcode found for namespace: " + NAMESPACE + " and municipalityId: OTHER_MUNICIPALITY_ID");

	}

	@Test
	void generateErrandNumber_alwaysUnique() {
		final var entity = new ErrandNumberSequenceEntity()
			.withNamespace(NAMESPACE)
			.withNamespace(MUNICIPALITY_ID)
			.withLastSequenceNumber(1234)
			.withResetYearMonth(dateFormatter.format(LocalDate.now().minusMonths(2)));
		final var maxCount = 99999;

		when(repository.findByNamespaceAndMunicipalityId(any(String.class), any(String.class))).thenReturn(Optional.of(entity));

		final var result = IntStream.range(0, maxCount).mapToObj(i ->
				stringGeneratorService.generateErrandNumber(NAMESPACE, MUNICIPALITY_ID))
			.toList();

		assertThat(result).hasSize(maxCount).doesNotHaveDuplicates();
		assertThat(result.get(result.size() - 1)).endsWith("99999");
	}

}
