package se.sundsvall.supportmanagement.integration.pwalkt;

import generated.se.sundsvall.pwalkt.ErrandEvent;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Hands errand events to pw-alkt, and decides what its answers mean.
 * <p>
 * The one place the relay meets the transport, which makes it the class to replace once the events travel over a
 * message queue instead.
 * <p>
 * Only a 422 is a refusal for good. It is the one answer that says something about the event rather than about the way
 * to pw-alkt. Everything else - a 5xx, a timeout, a 401 or a 404 from the gateway - is worth trying again: the row then
 * stays until whatever stood in the way has been fixed, and nothing is lost in the meantime.
 */
@Component
public class PwAlktIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(PwAlktIntegration.class);

	private final PwAlktClient client;

	public PwAlktIntegration(final PwAlktClient client) {
		this.client = client;
	}

	/**
	 * Hands an event to pw-alkt.
	 * <p>
	 * A refusal for good is logged here, since this is the one place the reason pw-alkt gave for it is known.
	 *
	 * @param  municipalityId             the municipality of the errand.
	 * @param  namespace                  the namespace of the errand.
	 * @param  errandEvent                the event.
	 * @return                            true when pw-alkt took the event, false when it refused the event for good.
	 * @throws PwAlktUnavailableException when the event did not go through and is worth trying again.
	 */
	public boolean sendErrandEvent(final String municipalityId, final String namespace, final ErrandEvent errandEvent) {
		try {
			client.handleErrandEvent(municipalityId, namespace, errandEvent);
			return true;
		} catch (final CallNotPermittedException e) {
			throw new PwAlktUnavailableException(true, e);
		} catch (final ThrowableProblem problem) {
			if (!isRefusalForGood(problem)) {
				throw new PwAlktUnavailableException(false, problem);
			}

			LOG.error("pw-alkt refused process event {} for errand {} for good, and it is not delivered again: no process is deployed under the key '{}'. {}",
				errandEvent.getEventId(), errandEvent.getErrandId(), sanitizeForLogging(errandEvent.getProcessKey()), sanitizeForLogging(problem.getDetail()));
			return false;
		} catch (final RuntimeException e) {
			throw new PwAlktUnavailableException(false, e);
		}
	}

	private static boolean isRefusalForGood(final ThrowableProblem problem) {
		return ofNullable(problem.getStatus())
			.map(status -> status.value() == UNPROCESSABLE_CONTENT.value())
			.orElse(false);
	}
}
