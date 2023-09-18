package se.sundsvall.supportmanagement.integration.db.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import se.sundsvall.supportmanagement.integration.db.ErrandNumberSequenceRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandNumberSequenceEntity;

@Component
public class ErrandNumberGeneratorService {

	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyMM");

	private final ErrandNumberSequenceRepository repository;

	public ErrandNumberGeneratorService(final ErrandNumberSequenceRepository errandNumberSequenceRepository) {
		this.repository = errandNumberSequenceRepository;
	}

	public String generateErrandNumber(final String namespace) {


		final var shortcode = NamespaceShortCode.findByNamespace(namespace);

		final var todayDate = dateFormatter.format(LocalDate.now());

		var sequence = repository.findById(1L).orElse(null);

		if (sequence == null) {
			sequence = new ErrandNumberSequenceEntity().withId(1L);
		}

		if (sequence.getResetYearMonth() != null && !sequence.getResetYearMonth().equals(todayDate)) {
			sequence.setResetYearMonth(todayDate);
			sequence.setLastSequenceNumber(1);
		} else {
			sequence.setLastSequenceNumber(sequence.getLastSequenceNumber() + 1);
		}

		repository.saveAndFlush(sequence);

		return "%s-%s%s".formatted(shortcode, todayDate, String.format("%04d", sequence.getLastSequenceNumber()));
	}

}
