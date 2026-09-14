package se.sundsvall.supportmanagement.integration.pwalkt;

import java.io.Serial;

/**
 * A delivery that did not go through but may well go through later: pw-alkt answered with something other than an
 * acceptance or a refusal for good, did not answer in time, could not be reached, or is being left alone by its circuit
 * breaker.
 * <p>
 * Thrown rather than returned, because what it means is that the transaction the delivery runs in must not be
 * committed. The row stays exactly as it was, and the next run tries again.
 */
public class PwAlktUnavailableException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final boolean circuitOpen;

	public PwAlktUnavailableException(final boolean circuitOpen, final Throwable cause) {
		super(circuitOpen
			? "The circuit breaker of pw-alkt is open"
			: "pw-alkt did not take the event: " + cause.getMessage(), cause);
		this.circuitOpen = circuitOpen;
	}

	/**
	 * @return whether pw-alkt was not called at all, since its circuit breaker is open. Every other event meets the same
	 *         answer until the breaker lets a call through again.
	 */
	public boolean isCircuitOpen() {
		return circuitOpen;
	}
}
