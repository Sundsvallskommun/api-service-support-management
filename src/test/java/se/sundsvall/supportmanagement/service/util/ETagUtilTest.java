package se.sundsvall.supportmanagement.service.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;
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

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
		" ", "\t"
	})
	void requiredPreconditionRejectsMissingOrBlankHeader(final String header) {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> ETagUtil.validateRequiredIfMatch(header, 7L))
			.satisfies(e -> assertThat(e.getStatus()).isEqualTo(PRECONDITION_REQUIRED));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"*", "W/\"7\"", "\"6\""
	})
	void requiredPreconditionRejectsWildcardWeakAndStaleTags(final String header) {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> ETagUtil.validateRequiredIfMatch(header, 7L))
			.satisfies(e -> assertThat(e.getStatus()).isEqualTo(PRECONDITION_FAILED));
	}

	@Test
	void requiredPreconditionAcceptsTheCurrentStrongTag() {
		assertThatNoException().isThrownBy(() -> ETagUtil.validateRequiredIfMatch("\"7\"", 7L));
	}
}
