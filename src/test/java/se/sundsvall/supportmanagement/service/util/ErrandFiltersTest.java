package se.sundsvall.supportmanagement.service.util;

import com.turkraft.springfilter.parser.FilterParser;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.ClassUtils;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class ErrandFiltersTest {

	// The parser of spring-filter, with the operators and functions it registers itself, so that the guard is asked about
	// the very trees a request is parsed into
	private static final List<String> PARSER_PACKAGES = List.of(
		"com.turkraft.springfilter.parser.", "com.turkraft.springfilter.definition.", "com.turkraft.springfilter.language.");

	private static AnnotationConfigApplicationContext springFilter;
	private static FilterParser filterParser;

	@BeforeAll
	static void startSpringFilter() {
		final var classLoader = ErrandFiltersTest.class.getClassLoader();
		springFilter = new AnnotationConfigApplicationContext();
		ImportCandidates.load(AutoConfiguration.class, classLoader).getCandidates().stream()
			.filter(name -> PARSER_PACKAGES.stream().anyMatch(name::startsWith))
			.map(name -> ClassUtils.resolveClassName(name, classLoader))
			.forEach(springFilter::register);
		springFilter.refresh();
		filterParser = springFilter.getBean(FilterParser.class);
	}

	@AfterAll
	static void stopSpringFilter() {
		springFilter.close();
	}

	@Test
	void noFilterPasses() {
		assertThatCode(() -> ErrandFilters.verifyFilterable(null)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"status:'NEW'",
		"stakeholders.firstName:'Anna' and parameters.values:'x'",
		"decisionsCount:1",
		"title~'%communications.%'",
		"title:'decisions' or description:'statements.x'"
	})
	void filtersThatStayOnTheErrandPass(final String filter) {
		final var filterNode = filterParser.parse(filter);

		assertThatCode(() -> ErrandFilters.verifyFilterable(filterNode)).doesNotThrowAnyException();
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
		"stakeholders.errandEntity.communications is not empty",
		// Further in, behind another operator
		"not (status:'NEW' or (title:'x' and communications.subject~'%x%'))",
		// Two filter parameters, as spring-filter joins them
		"(title:'x') and (communications.subject~'%secret%')",
		// Among the patterns of a like against a list, which spring-filter resolves as fields like any other
		"title ~ ['%', communications.subject]",
		"title ~~ ['%', decisions.justification]"
	})
	void filtersReachingAnIndexOnlyAssociationAreRefused(final String filter) {
		final var filterNode = filterParser.parse(filter);

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifyFilterable(filterNode));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getDetail()).matches("Filtering on '(communications|decisions|statements|investigations)' is not supported");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"created", "touched", "status", "errandNumber", "stakeholders.firstName", "parameters.values", "measures.type"
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
		"stakeholders.errandEntity.communications.messageBody",
		// Spelled as the repository resolves it rather than as it is named
		"Communications.subject",
		"communications_subject",
		"communicationsSubject"
	})
	void orderingsReachingAnIndexOnlyAssociationAreRefused(final String property) {
		final var pageable = PageRequest.of(0, 20, Sort.by(property).descending());

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifySortable(pageable));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getDetail()).matches("Sorting on '(communications|decisions|statements|investigations)' is not supported");
	}

	/** The entities are guarded on whichever association reaches them, not only on the associations of the errand. */
	@ParameterizedTest
	@ValueSource(strings = {
		"measures.decisionEntity.justification", "measures.statementEntity"
	})
	void orderingsReachingAGuardedEntityThroughAnotherAssociationAreRefused(final String property) {
		final var pageable = PageRequest.of(0, 20, Sort.by(property));

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifySortable(pageable));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getDetail()).matches("Sorting on '(decisionEntity|statementEntity)' is not supported");
	}

	@Test
	void orderingOnAPropertyTheErrandDoesNotHaveIsRefused() {
		final var pageable = PageRequest.of(0, 20, Sort.by("decisionsCount"));

		assertThrows(PropertyReferenceException.class, () -> ErrandFilters.verifySortable(pageable));
	}

	/** Every order of the sort is asked, not only the first. */
	@Test
	void anIndexOnlyAssociationBehindALegalOrderIsRefusedToo() {
		final var pageable = PageRequest.of(0, 20, Sort.by("created").and(Sort.by("communications.subject")));

		assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifySortable(pageable));
	}
}
