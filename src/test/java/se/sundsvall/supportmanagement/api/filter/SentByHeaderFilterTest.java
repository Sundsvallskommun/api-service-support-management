package se.sundsvall.supportmanagement.api.filter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zalando.problem.Problem;

@SpringBootTest(classes = SentByHeaderFilter.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SentByHeaderFilterTest {

	@MockitoBean
	private HttpServletRequest requestMock;

	@MockitoBean
	private HttpServletResponse responseMock;

	@MockitoBean
	private FilterChain filterChainMock;

	@Autowired
	private SentByHeaderFilter sentByHeaderFilter;

	/**
	 * Test scenario where a valid 'adAccount' header is provided.
	 */
	@Test
	void doFilterInternal_1() throws ServletException, IOException {
		final var headerValue = "and123and; type=adAccount";

		when(requestMock.getHeader(SENT_BY_HEADER)).thenReturn(headerValue);

		doAnswer((Answer<Object>) invocation -> {
			assertThat(sentByHeaderFilter.getSenderType()).isEqualTo("adAccount");
			assertThat(sentByHeaderFilter.getSenderId()).isEqualTo("and123and");
			return null;
		}).when(filterChainMock).doFilter(requestMock, responseMock);

		sentByHeaderFilter.doFilterInternal(requestMock, responseMock, filterChainMock);

		assertThat(sentByHeaderFilter.getSenderId()).isNull();
		assertThat(sentByHeaderFilter.getSenderType()).isNull();
		verify(requestMock).getHeader(SENT_BY_HEADER);
		verify(filterChainMock).doFilter(requestMock, responseMock);
	}

	/**
	 * Test scenario where a valid 'partyId' header is provided.
	 */
	@Test
	void doFilterInternal_2() throws ServletException, IOException {
		final var headerValue = "abfe217c-c196-4d81-a1fa-21066f54e680; type=partyId";

		when(requestMock.getHeader(SENT_BY_HEADER)).thenReturn(headerValue);

		doAnswer((Answer<Object>) invocation -> {
			assertThat(sentByHeaderFilter.getSenderType()).isEqualTo("partyId");
			assertThat(sentByHeaderFilter.getSenderId()).isEqualTo("abfe217c-c196-4d81-a1fa-21066f54e680");
			return null;
		}).when(filterChainMock).doFilter(requestMock, responseMock);

		sentByHeaderFilter.doFilterInternal(requestMock, responseMock, filterChainMock);

		assertThat(sentByHeaderFilter.getSenderId()).isNull();
		assertThat(sentByHeaderFilter.getSenderType()).isNull();
		verify(requestMock).getHeader(SENT_BY_HEADER);
		verify(filterChainMock).doFilter(requestMock, responseMock);
	}

	@ParameterizedTest
	@MethodSource(value = "doFilterInternalFailureArgumentProvider")
	void doFilterInternalFailure(final String headerValue, final String errorMessage) throws ServletException, IOException {

		when(requestMock.getHeader(SENT_BY_HEADER)).thenReturn(headerValue);

		assertThatThrownBy(() -> sentByHeaderFilter.doFilterInternal(requestMock, responseMock, filterChainMock))
			.isInstanceOf(Problem.class)
			.hasMessage(errorMessage);

		verify(requestMock).getHeader(SENT_BY_HEADER);
		verify(filterChainMock, never()).doFilter(requestMock, responseMock);
	}

	private static Stream<Arguments> doFilterInternalFailureArgumentProvider() {
		return Stream.of(
			Arguments.of("and123and; type=partyId", "Bad Request: Party id identifier must be a valid UUID"),
			Arguments.of("; type=partyId", "Bad Request: Party id identifier must be a valid UUID"),
			Arguments.of(" ; type=adAccount", "Bad Request: Ad account identifier cannot be blank"),
			Arguments.of("and123and; type=not-a-valid-type", "Bad Request: Invalid X-Sent-By type value"),
			Arguments.of(" ; type=adAccount", "Bad Request: Ad account identifier cannot be blank"));
	}

	@Test
	void validateTwoParts() {
		var string = "and123and; type=partyId";
		var parts = sentByHeaderFilter.validateTwoParts(string);

		assertThat(parts).isNotNull().hasSize(2);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"and123and; type=partyId; type=adAccount", "and123and type=adAccount"
	})
	void validateTwoPartsFailure(final String headerValue) {
		assertThatThrownBy(() -> sentByHeaderFilter.validateTwoParts(headerValue))
			.isInstanceOf(Problem.class)
			.hasMessage("Bad Request: Invalid X-Sent-By header value");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"type=partyId", "type=adAccount"
	})
	void validateType(final String type) {
		assertThatNoException().isThrownBy(() -> sentByHeaderFilter.validateType(type));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"type=not-a-valid-type", "partyId", "adAccount"
	})
	void validateTypeFailure(final String type) {
		assertThatThrownBy(() -> sentByHeaderFilter.validateType(type))
			.isInstanceOf(Problem.class)
			.hasMessage("Bad Request: Invalid X-Sent-By type value");
	}

	@ParameterizedTest
	@MethodSource(value = "validateIdentifierArgumentProvider")
	void validateIdentifier(final String type, final String identifier) {
		assertThatNoException().isThrownBy(() -> sentByHeaderFilter.validateIdentifier(type, identifier));
	}

	private static Stream<Arguments> validateIdentifierArgumentProvider() {
		return Stream.of(
			Arguments.of("partyId", "abfe217c-c196-4d81-a1fa-21066f54e680"),
			Arguments.of("adAccount", "and123and"));
	}

	@ParameterizedTest
	@MethodSource(value = "validateIdentifierFailureArgumentProvider")
	void validateIdentifierFailure(final String type, final String identifier, final String errorMessage) {
		assertThatThrownBy(() -> sentByHeaderFilter.validateIdentifier(type, identifier))
			.isInstanceOf(Problem.class)
			.hasMessage(errorMessage);
	}

	private static Stream<Arguments> validateIdentifierFailureArgumentProvider() {
		return Stream.of(
			Arguments.of("partyId", "and123and", "Bad Request: Party id identifier must be a valid UUID"),
			Arguments.of("adAccount", "", "Bad Request: Ad account identifier cannot be blank"),
			Arguments.of("not-a-valid-type", "and123and", "Internal Server Error: Should not be possible to reach this point"));
	}

}
