package se.sundsvall.supportmanagement.service.search.index;

import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

class SearchAvailabilityTest {

	@Test
	void enabled() {
		final var availability = new SearchAvailability(true);

		assertThat(availability.isEnabled()).isTrue();
		assertThatCode(availability::verifyEnabled).doesNotThrowAnyException();
	}

	@Test
	void disabled() {
		final var availability = new SearchAvailability(false);

		assertThat(availability.isEnabled()).isFalse();

		final var e = assertThrows(ThrowableProblem.class, availability::verifyEnabled);

		assertThat(e.getStatus()).isEqualTo(SERVICE_UNAVAILABLE);
		assertThat(e.getDetail()).isEqualTo(SearchAvailability.SEARCH_DISABLED);
	}
}
