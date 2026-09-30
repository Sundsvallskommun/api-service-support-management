package se.sundsvall.supportmanagement.api;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
		"investigations.summary:'x'",
		// Named without a field of their own, which tells whether the rows are there at all
		"communications is not empty",
		"communications is empty",
		"size(communications) > 0",
		"status:'NEW' and size(decisions) > 2",
		// Reached through the reference a child carries back to the errand, which is a path like any other to spring-filter
		"stakeholders.errandEntity.communications.messageBody~'%secret%'",
		"parameters.errandEntity.decisions.justification~'%x%'",
		"attachments.errandEntity.statements.responseText:'x'",
		"stakeholders.errandEntity.communications is not empty"
	})
	void filtersReachingAnIndexOnlyAssociationAreRefused(final String filter) {
		when(requestMock.getParameter(ErrandFilters.FILTER_PARAMETER)).thenReturn(filter);

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifyFilterable(requestMock));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getDetail()).matches("Filtering on '(communications|decisions|statements|investigations)' is not supported");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"created", "touched", "status", "errandNumber", "stakeholders.firstName", "parameters.values", "decisionsCount"
	})
	void orderingsThatStayOnTheErrandPass(final String property) {
		assertThatCode(() -> ErrandFilters.verifySortable(PageRequest.of(0, 20, Sort.by(property)))).doesNotThrowAnyException();
	}

	@Test
	void nothingToOrderByPasses() {
		assertThatCode(() -> ErrandFilters.verifySortable(null)).doesNotThrowAnyException();
		assertThatCode(() -> ErrandFilters.verifySortable(PageRequest.of(0, 20))).doesNotThrowAnyException();
	}

	/**
	 * An ordering is resolved into a join of the collection it names, so it reaches what a filter may not: the errands of
	 * a page are multiplied against the count beside it, and the listing is ordered by data guarded on a resource of its
	 * own.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"communications.messageBody",
		"decisions.justification",
		"statements.responseText",
		"investigations.summary",
		"communications",
		"stakeholders.errandEntity.communications.messageBody"
	})
	void orderingsReachingAnIndexOnlyAssociationAreRefused(final String property) {
		final var pageable = PageRequest.of(0, 20, Sort.by(property).descending());

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifySortable(pageable));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getDetail()).matches("Sorting on '(communications|decisions|statements|investigations)' is not supported");
	}

	/** Every order of the sort is asked, not only the first. */
	@Test
	void anIndexOnlyAssociationBehindALegalOrderIsRefusedToo() {
		final var pageable = PageRequest.of(0, 20, Sort.by("created").and(Sort.by("communications.subject")));

		assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifySortable(pageable));
	}

	@Test
	void countIsGuardedTheSameWay() {
		when(requestMock.getParameter(ErrandFilters.FILTER_PARAMETER)).thenReturn("communications.subject:'x'");

		assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifyFilterable(requestMock));
	}
}
