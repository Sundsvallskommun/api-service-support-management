package se.sundsvall.supportmanagement.integration.db.search;

import com.google.gson.JsonPrimitive;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OffsetDateTimeBinderTest {

	@Test
	void writesTheTimestampWithItsOffset() {
		final var bridge = new OffsetDateTimeBinder.Bridge();

		assertThat(bridge.toIndexedValue(OffsetDateTime.of(2025, 1, 10, 8, 0, 0, 123_000_000, ZoneOffset.ofHours(1)), null))
			.isEqualTo(new JsonPrimitive("2025-01-10T08:00:00.123+01:00"));
		assertThat(bridge.toIndexedValue(null, null)).isNull();
	}
}
