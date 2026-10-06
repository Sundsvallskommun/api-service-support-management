package se.sundsvall.supportmanagement.config.masking;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The fields whose text is kept in the payload log. Everything else is replaced.
 * <p>
 * An allowlist rather than a list of what to hide, because the two fail in different directions. A list of fields to
 * hide is only as current as the last person to remember it: a free text field added to the API, or one an older
 * revision carries under a name nobody recognises any more, is logged in full until someone notices. An allowlist
 * hides it until someone asks for it, and asking is what reviewing this list is.
 * <p>
 * dept44's own {@code logbook.body-filters} still applies, and runs first. The two are meant to be layered: this list
 * carries the policy, and a JSONPath filter there covers what a path to a field cannot express, such as a value under
 * a map key the client decides.
 *
 * @param keep        paths to the fields whose text is logged as it is, in the syntax {@link KeepRules} compiles.
 *                    Everything the API answers with that is not named here is replaced by {@link #placeholder()}:
 *                    not because it is known to carry personal data, but because nothing says it does not.
 * @param placeholder what replaces the text of a field that is not kept.
 * @param maxSize     longest body that is read at all, in characters. A longer one is replaced whole, without being
 *                    parsed: a body this size is one carrying a base64 encoded attachment, and reading it would build
 *                    a tree several times the size of the body itself, on the thread answering the request, to produce
 *                    a log entry the log server cannot take anyway - GELF chunking tops out around 65 kB. A page of a
 *                    hundred errands is around 180 kB, so the default is set above that and well below an attachment.
 */
@ConfigurationProperties(prefix = "payload-masking")
public record PayloadMaskingProperties(

	@DefaultValue({
	}) Set<String> keep,

	@DefaultValue("[masked]") String placeholder,

	@DefaultValue("262144") int maxSize) {
}
