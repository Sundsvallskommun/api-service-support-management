package se.sundsvall.supportmanagement.integration.db.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
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


	@Mock
	ErrandNumberSequenceRepository repository;

	@InjectMocks
	ErrandNumberGeneratorService stringGeneratorService;

	@Test
	void generateErrandNumber_resetSequence() {

		when(repository.findById(any(String.class))).thenReturn(Optional.of(new ErrandNumberSequenceEntity()
			.withLastSequenceNumber(1234)
			.withResetYearMonth(dateFormatter.format(LocalDate.now().minusMonths(2)))));

		final var result = stringGeneratorService.generateErrandNumber("CONTACTCENTER");

		assertThat(result).isEqualTo("KC-23090001");
	}

	@Test
	void generateErrandNumber_incrementSequence() {

		when(repository.findById(any(String.class))).thenReturn(Optional.of(new ErrandNumberSequenceEntity()
			.withLastSequenceNumber(123)
			.withResetYearMonth(dateFormatter.format(LocalDate.now()))));

		final var result = stringGeneratorService.generateErrandNumber("CONTACTCENTER");

		assertThat(result).isEqualTo("KC-23090124");
	}

	@Test
	void generateErrandNumber_noSequence() {

		when(repository.findById(any(String.class))).thenReturn(Optional.empty());

		final var result = stringGeneratorService.generateErrandNumber("CONTACTCENTER");

		assertThat(result).isEqualTo("KC-23090001");
	}


	@Test
	void generateErrandNumber_unknownNamespace() {

		assertThatThrownBy(() -> stringGeneratorService.generateErrandNumber("OTHER_NAMESPACE"))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("No namespace shortcode found for OTHER_NAMESPACE");

	}

	@Test
	void generateErrandNumber_alwaysUnique() {
		final var entity = new ErrandNumberSequenceEntity()
			.withLastSequenceNumber(1234)
			.withResetYearMonth(dateFormatter.format(LocalDate.now().minusMonths(2)));


		final var namespaces = List.of("CONTACTCENTER");
		final var maxCount = 99999;
		final var random = new Random();
		when(repository.findById(any(String.class))).thenReturn(Optional.of(entity));

		final var result = IntStream.range(0, maxCount).mapToObj(i ->
				stringGeneratorService.generateErrandNumber(namespaces.get(random.nextInt(namespaces.size()))))
			.toList();

		assertThat(result).hasSize(maxCount).doesNotHaveDuplicates();
		assertThat(result.get(result.size() - 1)).endsWith("99999");
	}

}
