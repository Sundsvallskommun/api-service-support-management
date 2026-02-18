package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import java.text.MessageFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandNumberParserTest {

	@ParameterizedTest
	@ValueSource(strings = {
		"PRH-2022-01", "PRH-2022-000001", "PH-2022-000001", "PRH-2022-011111111111", "##PRH-2022-011111111111", "asd"
	})
	void parseSubject(final String errandNumber) {

		final var subject = MessageFormat.format("Ärende #{0} Ansökan om bygglov för fastighet KATARINA 4", errandNumber);

		final var result = ErrandNumberParser.parseSubject(subject);

		assertThat(result).isEqualTo(errandNumber);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"PRH-2022-01", "PRH-2022-000001", "PH-2022-000001", "PRH-2022-011111111111", "##PRH-2022-011111111111", "asd"
	})
	void parseSubject_WithNoSpaceAfterErrandNUmber(final String errandNumber) {

		final var result = ErrandNumberParser.parseSubject(MessageFormat.format("Ärende #{0}", errandNumber));

		assertThat(result).isEqualTo(errandNumber);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"", "PRH-2022-01", "Ärende PRH-2022-000001 Ansökan"
	})
	void parseSubjectFaultyValues(final String subject) {

		final var result = ErrandNumberParser.parseSubject(subject);

		assertThat(result).isNull();
	}

}
