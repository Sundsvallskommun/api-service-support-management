package se.sundsvall.supportmanagement.service.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.format;

class ETagUtilTest {

	@Test
	void shouldFormatVersionAsQuotedString() {
		assertThat(format(0L)).isEqualTo("\"0\"");
		assertThat(format(7L)).isEqualTo("\"7\"");
		assertThat(format(Long.MAX_VALUE)).isEqualTo("\"" + Long.MAX_VALUE + "\"");
	}
}
