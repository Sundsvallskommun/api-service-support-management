package se.sundsvall.supportmanagement.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

class OffsetDateTimeSerializerTest {

	private static final OffsetDateTimeSerializer INSTANCE = OffsetDateTimeSerializer.create();

	@Test
	void create() {
		assertThat(INSTANCE).isNotNull().isInstanceOf(OffsetDateTimeSerializer.class);
	}

	@Test
	void shouldSkipFieldForDeclaredMethodInDeclaredClass() {
		final var offsetDateTime = OffsetDateTime.now();
		final var serialized = INSTANCE.serialize(offsetDateTime, getClass(), null);

		assertThat(serialized.getAsString()).isEqualTo(offsetDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
	}
}
