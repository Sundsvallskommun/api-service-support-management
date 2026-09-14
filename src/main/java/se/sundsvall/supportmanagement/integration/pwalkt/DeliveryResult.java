package se.sundsvall.supportmanagement.integration.pwalkt;

/**
 * What pw-alkt made of an event that reached it.
 *
 * @param rejected whether the event was refused for good. That is what a 422 says - the process key matches no process
 *                 pw-alkt has deployed - and sending the event again changes nothing.
 * @param detail   the reason pw-alkt gave for refusing the event, or null for an accepted one.
 */
public record DeliveryResult(boolean rejected, String detail) {

	public static final DeliveryResult ACCEPTED = new DeliveryResult(false, null);

	public static DeliveryResult rejection(final String detail) {
		return new DeliveryResult(true, detail);
	}
}
