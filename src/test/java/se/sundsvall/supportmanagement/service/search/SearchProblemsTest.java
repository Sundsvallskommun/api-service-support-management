package se.sundsvall.supportmanagement.service.search;

import java.net.ConnectException;
import java.time.Duration;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

class SearchProblemsTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	@Test
	void aSearchGivenUpOnTellsWhatToDoAboutIt() {
		final var problem = (ThrowableProblem) SearchProblems.toProblem(new SearchTimeoutException("HSEARCH400xxx: timed out", null), TIMEOUT);

		assertThat(problem.getStatus()).isEqualTo(GATEWAY_TIMEOUT);
		assertThat(problem.getDetail()).isEqualTo("The search took longer than 10 seconds and was given up on. Narrow it, or use fewer wildcards");
	}

	@Test
	void parseFailureIsTheClients() {
		final var exception = new SearchException("""
			HSEARCH400007: Elasticsearch request failed: HSEARCH400090: Elasticsearch response indicates a failure.
			Response: 400 'Bad Request' with body
			{
			  "error": {
			    "root_cause": [
			      {
			        "type": "query_shard_exception",
			        "reason": "Failed to parse query [title:(unbalanced]",
			        "index": "errand-000001"
			      }
			    ]
			  }
			}""");

		final var result = SearchProblems.toProblem(exception, TIMEOUT);

		assertThat(result).isInstanceOf(ThrowableProblem.class);
		final var problem = (ThrowableProblem) result;
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getDetail()).isEqualTo("The search query could not be parsed: Failed to parse query [title:(unbalanced]");
	}

	@Test
	void escapedQuotesInTheReasonAreUnescaped() {
		final var exception = new SearchException("""
			{"error":{"root_cause":[{"type":"parse_exception","reason":"failed to parse date field [2025-02-01] with format [\\"strict\\"]"}]}}""");

		final var problem = (ThrowableProblem) SearchProblems.toProblem(exception, TIMEOUT);

		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getDetail()).isEqualTo("The search query could not be parsed: failed to parse date field [2025-02-01] with format [\"strict\"]");
	}

	@Test
	void unreachableClusterIsWorthRetrying() {
		final var exception = new SearchException("HSEARCH400007: Elasticsearch request failed: Connection refused", new ConnectException("Connection refused"));

		final var problem = (ThrowableProblem) SearchProblems.toProblem(exception, TIMEOUT);

		assertThat(problem.getStatus()).isEqualTo(SERVICE_UNAVAILABLE);
		assertThat(problem.getDetail()).isEqualTo(SearchProblems.SEARCH_UNAVAILABLE);
	}

	@Test
	void anythingElseIsOurs() {
		final var exception = new SearchException("HSEARCH000001: something else entirely");

		assertThat(SearchProblems.toProblem(exception, TIMEOUT)).isSameAs(exception);
	}
}
