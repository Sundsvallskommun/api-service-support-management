package se.sundsvall.supportmanagement.service.util;

import com.turkraft.springfilter.parser.FilterParser;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.ClassUtils;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.model.enums.ProtectedResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

class ErrandFiltersTest {

	// The parser of spring-filter, with the operators and functions it registers itself, so that the guard is asked about
	// the very trees a request is parsed into
	private static final List<String> PARSER_PACKAGES = List.of(
		"com.turkraft.springfilter.parser.", "com.turkraft.springfilter.definition.", "com.turkraft.springfilter.language.");

	private static final Predicate<ProtectedResource> EVERY_RESOURCE = _ -> true;
	private static final String USER = "joe01doe";

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
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(USER));
	}

	@AfterAll
	static void stopSpringFilter() {
		springFilter.close();
		Identifier.remove();
	}

	@Test
	void noFilterPasses() {
		assertThatCode(() -> ErrandFilters.verifyFilterable(null, EVERY_RESOURCE)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"status:'NEW'",
		"stakeholders.firstName:'Anna' and parameters.values:'x'",
		"title~'%communications.%'",
		"title:'decisions' or description:'statements.x'",
		"measures.goal~'%x%' and timeMeasures.description:'x' and notifications.ownerId:'x'",
		"attachments.fileName:'x.pdf' and jsonParameters.key:'x'",
		// Back to the errand and out again, to something the errand may be filtered on
		"stakeholders.errandEntity.title:'x'"
	})
	void filtersThatReachNothingGuardedPass(final String filter) {
		final var filterNode = filterParser.parse(filter);

		assertThatCode(() -> ErrandFilters.verifyFilterable(filterNode, EVERY_RESOURCE)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
		communications.messageBody~'%x%'                                        | communications
		status:'NEW' and decisions.description~'%x%'                            | decisions
		statements.description:'x'                                              | statements
		investigations.description:'x'                                          | investigations
		communications is not empty                                             | communications
		communications is empty                                                 | communications
		size(communications) > 0                                                | communications
		status:'NEW' and size(decisions) > 2                                    | decisions
		stakeholders.errandEntity.communications.messageBody~'%secret%'         | stakeholders.errandEntity.communications
		parameters.errandEntity.decisions.description~'%x%'                     | parameters.errandEntity.decisions
		stakeholders.errandEntity.communications is not empty                   | stakeholders.errandEntity.communications
		measures.decisionEntity.description~'%x%'                               | measures.decisionEntity
		measures.statementEntity.description:'x'                                | measures.statementEntity
		measures.decisionEntity.investigationEntity.description:'x'             | measures.decisionEntity
		not (status:'NEW' or (title:'x' and communications.subject~'%x%'))      | communications
		(title:'x') and (communications.subject~'%secret%')                     | communications
		""")
	void filtersReachingAnIndexOnlyEntityAreRefused(final String filter, final String reaching) {
		final var filterNode = filterParser.parse(filter);

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifyFilterable(filterNode, EVERY_RESOURCE));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getDetail()).isEqualTo("Filtering on '%s' is not supported".formatted(reaching));
	}

	/** What cannot be resolved from the errand cannot be told to reach nothing guarded. */
	@Test
	void aFilterThatCannotBeResolvedIsRefused() {
		final var filterNode = filterParser.parse("noSuchProperty:'x'");

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifyFilterable(filterNode, EVERY_RESOURCE));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getDetail()).isEqualTo("Filtering on 'noSuchProperty' is not supported");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
		measures.goal~'%x%'                         | MEASURE
		size(measures) > 0                          | MEASURE
		measures.jsonParameters.key:'x'             | MEASURE
		timeMeasures.description:'x'                | TIME_MEASURE
		notifications.ownerId:'x'                   | NOTIFICATION
		attachments.fileName:'x.pdf'                | ATTACHMENT
		parameters.values:'x'                       | PARAMETER
		jsonParameters.key:'x'                      | JSON_PARAMETER
		stakeholders.errandEntity.measures.goal:'x' | MEASURE
		""")
	void filtersReachingAResourceTheUserDoesNotReachAreRefused(final String filter, final ProtectedResource resource) {
		final var filterNode = filterParser.parse(filter);
		final var everyOtherResource = EnumSet.complementOf(EnumSet.of(resource));

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifyFilterable(filterNode, everyOtherResource::contains));

		assertThat(e.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(e.getDetail()).isEqualTo("Resource '%s' not filterable by user '%s'".formatted(resource.getPath(), USER));
	}

	/** A resource the user reaches does not open what hangs off it and is guarded on a resource of its own. */
	@Test
	void anAttachmentOfAMeasureIsHeldToTheAttachmentsToo() {
		final var filterNode = filterParser.parse("measures.attachments.fileName:'x.pdf'");

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifyFilterable(filterNode, EnumSet.of(ProtectedResource.MEASURE)::contains));

		assertThat(e.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(e.getDetail()).isEqualTo("Resource 'errand/attachment' not filterable by user '%s'".formatted(USER));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"created", "touched", "status", "errandNumber", "stakeholders.firstName", "parameters.values", "measures.goal"
	})
	void orderingsThatReachNothingClosedPass(final String property) {
		assertThatCode(() -> ErrandFilters.verifySortable(PageRequest.of(0, 20, Sort.by(property)), EVERY_RESOURCE)).doesNotThrowAnyException();
	}

	@Test
	void nothingToOrderByPasses() {
		assertThatCode(() -> ErrandFilters.verifySortable(null, EVERY_RESOURCE)).doesNotThrowAnyException();
		assertThatCode(() -> ErrandFilters.verifySortable(PageRequest.of(0, 20), EVERY_RESOURCE)).doesNotThrowAnyException();
	}

	/** Refused by Spring Data itself, before anything is read. */
	@Test
	void anOrderingThatCannotBeResolvedIsLeftToSpringData() {
		assertThatCode(() -> ErrandFilters.verifySortable(PageRequest.of(0, 20, Sort.by("noSuchProperty")), EVERY_RESOURCE)).doesNotThrowAnyException();
	}

	/**
	 * An ordering is resolved into a join of the collection it names, so it reaches what a filter may not: the errands of
	 * a page are multiplied against the count beside it, and the listing is ordered by data guarded on a resource of its
	 * own. Spring Data reads a property such as communicationsSubject as communications.subject, which is no detour around
	 * the guard either.
	 */
	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
		communications.messageBody                          | communications
		decisions.description                               | decisions
		statements.description                              | statements
		investigations.description                          | investigations
		communications                                      | communications
		stakeholders.errandEntity.communications.messageBody | stakeholders.errandEntity.communications
		measures.decisionEntity.description                 | measures.decisionEntity
		communicationsSubject                               | communications
		""")
	void orderingsReachingAnIndexOnlyEntityAreRefused(final String property, final String reaching) {
		final var pageable = PageRequest.of(0, 20, Sort.by(property).descending());

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifySortable(pageable, EVERY_RESOURCE));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getDetail()).isEqualTo("Sorting on '%s' is not supported".formatted(reaching));
	}

	@Test
	void anOrderingReachingAResourceTheUserDoesNotReachIsRefused() {
		final var pageable = PageRequest.of(0, 20, Sort.by("measures.goal"));

		final var e = assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifySortable(pageable, _ -> false));

		assertThat(e.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(e.getDetail()).isEqualTo("Resource 'errand/measure' not sortable by user '%s'".formatted(USER));
	}

	/** Every order of the sort is asked, not only the first. */
	@Test
	void anIndexOnlyEntityBehindALegalOrderIsRefusedToo() {
		final var pageable = PageRequest.of(0, 20, Sort.by("created").and(Sort.by("communications.subject")));

		assertThrows(ThrowableProblem.class, () -> ErrandFilters.verifySortable(pageable, EVERY_RESOURCE));
	}
}
