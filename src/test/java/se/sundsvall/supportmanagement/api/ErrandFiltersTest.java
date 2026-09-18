package se.sundsvall.supportmanagement.api;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class ErrandFiltersTest {

	@Mock
	private HttpServletRequest requestMock;

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"",
		"status:'NEW'",
		"stakeholders.firstName:'Anna' and parameters.values:'x'",
		"decisionsCount:1",
		"title~'%communications.%'"
	})
	void filtersThatStayOnTheErrandPass(final String filter) {
		when(requestMock.getParameter(ErrandFilters.FILTER_PARAMETER)).thenReturn(filter);

		assertThatCode(() -> ErrandFilters.verifyFilterable(requestMock)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"communications.messageBody~'%x%'",
		"status:'NEW' and decisions.justification~'%x%'",
		"statements.responseText:'x'",
		"investigations.summary:'x'"
	})
	void filtersReachingAnIndexOnlyAssociationAreRefused(final String filter) {
		when(requestMock.getParameter(ErrandFilters.FILTER_PARAMETER)).thenReturn(filter);

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifyFilterable(requestMock));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getDetail()).matches("Filtering on '(communications|decisions|statements|investigations)' is not supported");
	}

	@Test
	void countIsGuardedTheSameWay() {
		when(requestMock.getParameter(ErrandFilters.FILTER_PARAMETER)).thenReturn("communications.subject:'x'");

		assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifyFilterable(requestMock));
	}
}
