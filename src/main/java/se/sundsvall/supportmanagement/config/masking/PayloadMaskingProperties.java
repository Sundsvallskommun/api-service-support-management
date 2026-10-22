package se.sundsvall.supportmanagement.config.masking;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The fields whose values are kept in the payload log. Everything else is replaced.
 * <p>
 * An allowlist, so that a field added to the API, or one an older revision carries under a name nobody recognises any
 * more, is withheld until someone asks for it rather than logged until someone remembers it.
 * <p>
 * dept44's own {@code logbook.body-filters} still applies, and runs first. The two are meant to be layered: this list
 * carries the policy, and a JSONPath filter there covers what a path to a field cannot express, such as a value under
 * a map key the client decides.
 *
 * @param keep        paths to the fields whose values are logged as they are, in the syntax {@link KeepRules}
 *                    compiles. A string or a number that is not named here is replaced by {@link #placeholder()}: not
 *                    because it is known to carry personal data, but because nothing says it does not. Property names,
 *                    booleans and nulls are kept whatever the field.
 * @param placeholder what replaces the value of a field that is not kept.
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
