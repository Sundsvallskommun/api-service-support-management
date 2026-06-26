package se.sundsvall.supportmanagement.service.util;

import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.format;

class ETagUtilTest {

	@Test
	void shouldFormatVersionAsQuotedString() {
		assertThat(format(0L)).isEqualTo("\"0\"");
		assertThat(format(7L)).isEqualTo("\"7\"");
		assertThat(format(Long.MAX_VALUE)).isEqualTo("\"" + Long.MAX_VALUE + "\"");
	}

	@Test
	void shouldPassWhenIfMatchIsNull() {
		assertThatNoException().isThrownBy(() -> ETagUtil.validateIfMatch(null, 7L));
	}

	@Test
	void shouldPassWhenIfMatchIsStar() {
		assertThatNoException().isThrownBy(() -> ETagUtil.validateIfMatch("*", 7L));
	}

	@Test
	void shouldPassWhenVersionMatches() {
		assertThatNoException().isThrownBy(() -> ETagUtil.validateIfMatch("\"7\"", 7L));
	}

	@Test
	void shouldPassWhenOneTagInListMatches() {
		assertThatNoException().isThrownBy(() -> ETagUtil.validateIfMatch("\"5\", \"7\"", 7L));
	}

	@Test
	void shouldThrowOn412WhenVersionMismatches() {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> ETagUtil.validateIfMatch("\"5\"", 7L))
			.satisfies(e -> assertThat(e.getStatus()).isEqualTo(PRECONDITION_FAILED));
	}

	@Test
	void shouldThrowOn412WhenWeakETag() {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> ETagUtil.validateIfMatch("W/\"7\"", 7L))
			.satisfies(e -> assertThat(e.getStatus()).isEqualTo(PRECONDITION_FAILED));
	}
}
