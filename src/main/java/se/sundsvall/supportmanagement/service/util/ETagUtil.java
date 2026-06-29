package se.sundsvall.supportmanagement.service.util;

public final class ETagUtil {

	private ETagUtil() {}

	public static String format(final long version) {
		return "\"" + version + "\"";
	}
}
