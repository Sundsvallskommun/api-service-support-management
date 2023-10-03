package se.sundsvall.supportmanagement.integration.db.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.supportmanagement.integration.db.ErrandNumberSequenceRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandNumberSequenceEntity;

@Component
public class ErrandNumberGeneratorService {

	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyMM");

	private final ErrandNumberSequenceRepository repository;

	public ErrandNumberGeneratorService(final ErrandNumberSequenceRepository errandNumberSequenceRepository) {
		this.repository = errandNumberSequenceRepository;
	}

	@Transactional(isolation = Isolation.SERIALIZABLE)
	public String generateErrandNumber(final String namespace, final String municipalityId) {


		final var shortcode = NamespaceShortCode.findByNamespace(namespace, municipalityId);

		final var todayDate = dateFormatter.format(LocalDate.now());

		var sequence = repository.findByNamespaceAndMunicipalityId(namespace, municipalityId).orElse(null);

		if (sequence == null) {
			sequence = new ErrandNumberSequenceEntity()
				.withNamespace(namespace)
				.withMunicipalityId(municipalityId)
				.withLastSequenceNumber(0)
				.withResetYearMonth(todayDate);
		}

		if (!todayDate.equals(sequence.getResetYearMonth())) {
			sequence.setResetYearMonth(todayDate);
			sequence.setLastSequenceNumber(1);
		} else {
			sequence.setLastSequenceNumber(sequence.getLastSequenceNumber() + 1);
		}

		repository.saveAndFlush(sequence);

		return "%s-%s%s".formatted(shortcode, todayDate, String.format("%04d", sequence.getLastSequenceNumber()));
	}

}
