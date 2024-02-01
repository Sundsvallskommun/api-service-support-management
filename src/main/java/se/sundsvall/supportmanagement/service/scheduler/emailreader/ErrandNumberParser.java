package se.sundsvall.supportmanagement.service.scheduler.emailreader;

public final class ErrandNumberParser {

	private ErrandNumberParser() {}


	/**
	 * Parses the subject of an email and returns the errand number if it exists.
	 * The errand number is expected to be in the format: PRH-2022-000001
	 * The errand number is expected to be preceded by a '#' and followed by a space or the end of the string.
	 * If the errand number is not found, null is returned.
	 *
	 * @param subject the subject of the email to parse
	 * @return the errand number if it exists, otherwise null
	 */
	public static String parseSubject(final String subject) {
		if (subject == null || subject.isEmpty()) {
			return null;
		}

		final var hashIndex = subject.indexOf("#");

		if (hashIndex != -1) {
			final var spaceAfterHashIndex = subject.indexOf(" ", hashIndex);

			if (spaceAfterHashIndex != -1) {
				return subject.substring(hashIndex + 1, spaceAfterHashIndex);
			} else {
				// If there is no space after '#', return the substring from '#' to the end
				return subject.substring(hashIndex + 1);
			}
		}
		return null;
	}

}
