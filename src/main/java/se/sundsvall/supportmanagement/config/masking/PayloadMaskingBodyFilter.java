package se.sundsvall.supportmanagement.config.masking;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.logbook.BodyFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static java.util.Objects.isNull;

/**
 * Replaces the values a JSON payload carries before it is logged, keeping only the fields
 * {@link PayloadMaskingProperties#keep()} names.
 * <p>
 * Registered as a {@link BodyFilter} bean, which dept44's Logbook configuration collects and applies to every request
 * and response it logs, ours and the ones made to the services around us. The body is parsed once, walked once and
 * written once, whatever the number of rules involved.
 * <p>
 * The contract:
 * <ul>
 * <li>a string or a number is logged where a rule names its field, and replaced with
 * {@link PayloadMaskingProperties#placeholder()} where none does;</li>
 * <li>every property name is kept, so a log line still shows the shape of what was sent;</li>
 * <li>a boolean and a null are kept: one bit under a name that is already visible names nobody;</li>
 * <li>a body of more than {@link PayloadMaskingProperties#maxSize()} characters is never parsed, and its length is
 * logged instead;</li>
 * <li>a body that is not JSON is passed on untouched, and one that cannot be read is replaced whole.</li>
 * </ul>
 */
class PayloadMaskingBodyFilter implements BodyFilter {

	private static final Logger LOG = LoggerFactory.getLogger(PayloadMaskingBodyFilter.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** What Logbook requires of a JSON content type before the rest of the header is read. */
	private static final String APPLICATION_TYPE = "application/";

	/** What replaces a body that is not logged at all. A JSON document, so that the entry it lands in stays one. */
	private static final String UNREADABLE_BODY = "{\"masked\":\"unreadable\"}";
	private static final String TOO_LARGE_BODY = "{\"masked\":\"too large\",\"characters\":%d}";

	private final KeepRules keep;
	private final String placeholder;
	private final int maxSize;

	PayloadMaskingBodyFilter(final PayloadMaskingProperties properties) {
		this.keep = KeepRules.of(properties.keep());
		this.placeholder = properties.placeholder();
		this.maxSize = properties.maxSize();
	}

	@Override
	public String filter(final String contentType, final String body) {
		if (isNull(body) || body.isBlank() || !isJson(contentType)) {
			return body;
		}

		// Answered before the body is read, so that nothing the size of an attachment is parsed. A tree of a body is
		// several times the body, which is the cost worth avoiding here; the filters dept44 puts before this one have
		// already scanned and copied the string by the time it arrives. The length is what is logged instead, since
		// that is the one thing worth knowing about a body too big to log.
		if (body.length() > maxSize) {
			return TOO_LARGE_BODY.formatted(body.length());
		}

		try {
			final var root = MAPPER.readTree(body);
			// A body that is nothing but a string has no field to be kept by, so nothing keeps it. Written back as a
			// string rather than as the word alone, so that what replaces it is still a document
			if (root.isString()) {
				return MAPPER.writeValueAsString(placeholder);
			}
			mask(root, new ArrayList<>());
			return root.toString();
		} catch (final Exception e) {
			// A body that cannot be read cannot be masked either, and a log entry is not worth failing a request over.
			// Answering with the body as it came would be answering with what this class exists to withhold, so what
			// is logged is that there was something here and that it could not be read.
			LOG.warn("Could not mask payload for logging ({}), replacing it", e.toString());
			return UNREADABLE_BODY;
		}
	}

	/**
	 * @param node the node to mask in place.
	 * @param path the fields from the root of the body down to this node, used as a stack. An array is not a field of
	 *             its own: its elements are at the path of the array, which is what makes the strings of
	 *             {@code recipients} askable as {@code recipients} and an object inside {@code metadata} askable as
	 *             {@code metadata.value}.
	 */
	private void mask(final JsonNode node, final List<String> path) {
		if (node instanceof final ObjectNode object) {
			maskProperties(object, path);
		} else if (node instanceof final ArrayNode array) {
			maskElements(array, path);
		}
	}

	private void maskProperties(final ObjectNode object, final List<String> path) {
		// Collected and replaced afterwards rather than as they are found, so that nothing is written to the object
		// while it is being read
		final var replace = new ArrayList<String>();
		object.properties().forEach(property -> {
			final var value = property.getValue();
			path.add(property.getKey());
			if (value.isObject() || value.isArray()) {
				mask(value, path);
			} else if (mustBeNamedToBeLogged(value) && !keep.keeps(path)) {
				replace.add(property.getKey());
			}
			path.removeLast();
		});
		replace.forEach(name -> object.put(name, placeholder));
	}

	private void maskElements(final ArrayNode array, final List<String> path) {
		for (var i = 0; i < array.size(); i++) {
			final var element = array.get(i);
			if (element.isObject() || element.isArray()) {
				mask(element, path);
			} else if (mustBeNamedToBeLogged(element) && !keep.keeps(path)) {
				array.set(i, placeholder);
			}
		}
	}

	/**
	 * Whether a value is one a rule has to name for it to be logged. A number is, because a personal identity number
	 * and a coordinate are numbers; a boolean and a null are not, since one bit under a name that is already visible
	 * names nobody, and asking for every flag by name would say nothing and cost the whole list.
	 */
	private static boolean mustBeNamedToBeLogged(final JsonNode value) {
		return value.isString() || value.isNumber();
	}

	/**
	 * Whether Logbook will log this body as JSON, read the way Logbook reads it: by the text of the header, not by
	 * parsing it. A content type cannot be parsed into a {@link org.springframework.http.MediaType} and judged here
	 * instead, because a malformed parameter - {@code application/json;charset=bogus} is enough - makes that throw,
	 * and a body this returned false for is logged exactly as it came. Logbook's own {@code ContentType} asks only
	 * what the header says, so a client that sends nonsense after the semicolon would have had its payload logged
	 * unmasked.
	 * <p>
	 * A problem is answered as {@code application/problem+json}, whose detail is as free as any other text, so every
	 * {@code +json} subtype counts.
	 */
	private static boolean isJson(final String contentType) {
		if (isNull(contentType)) {
			return false;
		}
		final var type = contentType.toLowerCase(Locale.ROOT);
		if (!type.startsWith(APPLICATION_TYPE)) {
			return false;
		}
		final var parameters = type.indexOf(';');
		final var mimeType = (parameters < 0 ? type : type.substring(0, parameters)).trim();
		return mimeType.endsWith("/json") || mimeType.endsWith("+json");
	}
}
