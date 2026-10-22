package se.sundsvall.supportmanagement.integration.pwalkt;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import generated.se.sundsvall.pwalkt.ErrandEvent;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import se.sundsvall.dept44.exception.ClientProblem;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@ExtendWith(MockitoExtension.class)
class PwAlktIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ALKT";
	private static final String REFUSAL = "no process is deployed under alkt-ansokan";

	private final ErrandEvent errandEvent = new ErrandEvent().eventId("event-1").errandId("errand-1").processKey("alkt-ansokan");

	private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

	@Mock
	private PwAlktClient clientMock;

	@InjectMocks
	private PwAlktIntegration integration;

	/**
	 * What reaches the integration for everything that is not a 422: the error decoder turns every other answer into a
	 * problem with bad gateway as its status, and a transport failure arrives as whatever the client threw.
	 */
	private static Stream<RuntimeException> failuresWorthTryingAgain() {
		return Stream.of(
			new ServerProblem(BAD_GATEWAY, "pw-alkt error: {status=503 Service Unavailable}"),
			new ClientProblem(BAD_GATEWAY, "pw-alkt error: {status=401 Unauthorized}"),
			Problem.valueOf(BAD_GATEWAY, "pw-alkt error: {status=302 Found}"),
			new IllegalStateException("Read timed out"));
	}

	@BeforeEach
	void setUp() {
		logAppender.start();
		logger().addAppender(logAppender);
	}

	@AfterEach
	void tearDown() {
		logger().detachAppender(logAppender);
	}

	@Test
	@DisplayName("Verification that an event pw-alkt takes in is reported as taken")
	void anAcceptedEvent() {
		when(clientMock.handleErrandEvent(MUNICIPALITY_ID, NAMESPACE, errandEvent)).thenReturn(ResponseEntity.accepted().build());

		assertThat(integration.sendErrandEvent(MUNICIPALITY_ID, NAMESPACE, errandEvent)).isTrue();
		assertThat(logAppender.list).isEmpty();
	}

	@Test
	@DisplayName("Verification that a 422 is a refusal for good, returned rather than thrown and logged with the reason pw-alkt gave")
	void aRefusalForGood() {
		when(clientMock.handleErrandEvent(MUNICIPALITY_ID, NAMESPACE, errandEvent)).thenThrow(new ClientProblem(UNPROCESSABLE_CONTENT, REFUSAL));

		assertThat(integration.sendErrandEvent(MUNICIPALITY_ID, NAMESPACE, errandEvent)).isFalse();
		assertThat(logAppender.list)
			.singleElement()
			.satisfies(event -> {
				assertThat(event.getLevel()).isEqualTo(Level.ERROR);
				assertThat(event.getFormattedMessage()).contains("event-1", "errand-1", "alkt-ansokan", REFUSAL);
			});
	}

	@ParameterizedTest
	@MethodSource("failuresWorthTryingAgain")
	@DisplayName("Verification that every other failure is worth trying again, since the row then stays until whatever stood in the way is fixed")
	void everyOtherFailureIsWorthTryingAgain(final RuntimeException failure) {
		when(clientMock.handleErrandEvent(MUNICIPALITY_ID, NAMESPACE, errandEvent)).thenThrow(failure);

		assertThatExceptionOfType(PwAlktUnavailableException.class)
			.isThrownBy(() -> integration.sendErrandEvent(MUNICIPALITY_ID, NAMESPACE, errandEvent))
			.satisfies(exception -> {
				assertThat(exception.isCircuitOpen()).isFalse();
				assertThat(exception.getCause()).isSameAs(failure);
				assertThat(exception.getMessage()).isEqualTo("pw-alkt did not take the event: " + failure.getMessage());
			});
	}

	@Test
	@DisplayName("Verification that an open circuit breaker says so, since every other event would meet the same answer")
	void anOpenCircuitBreaker() {
		when(clientMock.handleErrandEvent(MUNICIPALITY_ID, NAMESPACE, errandEvent)).thenThrow(CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("pw-alkt")));

		assertThatExceptionOfType(PwAlktUnavailableException.class)
			.isThrownBy(() -> integration.sendErrandEvent(MUNICIPALITY_ID, NAMESPACE, errandEvent))
			.withMessage("The circuit breaker of pw-alkt is open")
			.satisfies(exception -> assertThat(exception.isCircuitOpen()).isTrue());
	}

	private static Logger logger() {
		return (Logger) LoggerFactory.getLogger(PwAlktIntegration.class);
	}
}
