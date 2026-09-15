package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.GsonBuilder;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDateSerializerTest {

	@Test
	void serializeWritesAnIsoDate() {

		// Act
		final var result = LocalDateSerializer.create().serialize(LocalDate.of(2026, 9, 10), LocalDate.class, null);

		// Verify
		assertThat(result.getAsString()).isEqualTo("2026-09-10");
	}

	/**
	 * The point of the serializer is that Gson never reflects into {@link LocalDate}, which is closed to reflection on a
	 * modern JDK. Registering it and writing a date is what proves that.
	 */
	@Test
	void gsonWritesADateWithoutReflectingIntoIt() {

		// Arrange
		final var gson = new GsonBuilder()
			.registerTypeAdapter(LocalDate.class, LocalDateSerializer.create())
			.create();

		// Act
		final var result = gson.toJson(LocalDate.of(2021, 1, 31));

		// Verify
		assertThat(result).isEqualTo("\"2021-01-31\"");
	}
}
