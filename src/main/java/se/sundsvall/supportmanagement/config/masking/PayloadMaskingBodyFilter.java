package se.sundsvall.supportmanagement.config.masking;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.zalando.logbook.BodyFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static java.util.Objects.isNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Replaces the text a JSON payload carries before it is logged, keeping only the fields
 * {@link PayloadMaskingProperties#keep()} names.
 * <p>
 * Registered as a {@link BodyFilter} bean, which dept44's Logbook configuration collects and applies to every request
 * and response it logs, ours and the ones made to the services around us.
 * <p>
 * The body is parsed once, walked once and written once, whatever the number of rules involved. The JSONPath filters
 * dept44 builds from {@code logbook.body-filters} parse and write the whole body once per path, which measured about
 * 160 times this for fifty rules on a small errand, and most of half a second for a page of a hundred. Both run on the
 * thread that is answering the request.
 * <p>
 * Kept as it is, regardless of the field it belongs to: every property name, so a log line still shows the shape of
 * what was sent, and every number, boolean and null, which carry sizes, counts, versions and flags rather than text.
 * A personal identity number written as a number rather than a string is therefore not caught here - that is what
 * {@code dept44.logback.pii-masking} is under this.
 */
class PayloadMaskingBodyFilter implements BodyFilter {

	private static final Logger LOG = LoggerFactory.getLogger(PayloadMaskingBodyFilter.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * What replaces a body that is not logged at all. A JSON document rather than a bare word, because the formatter
	 * embeds a body it takes for JSON into the log entry as it is, without quoting or checking it: Logbook's
	 * {@code JsonHttpLogFormatter} is built here without body validation, and its {@code looksLikeJson} asks only
	 * whether the first and last character could begin and end one. A plain {@code [masked]} passes that test on its
	 * brackets alone and leaves the entry unparseable, which costs the entry its fields wherever the log server reads
	 * them as JSON.
	 */
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

		// Answered before the body is read, so that nothing the size of an attachment is parsed or copied. A string is
		// immutable: every filter that reads one and writes one back holds both at once, and a tree of a body is
		// several times the body. The length is what is logged instead, since that is the one thing worth knowing
		// about a body too big to log.
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
			} else if (value.isString() && !keep.keeps(path)) {
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
			} else if (element.isString() && !keep.keeps(path)) {
				array.set(i, placeholder);
			}
		}
	}

	private static boolean isJson(final String contentType) {
		if (isNull(contentType)) {
			return false;
		}
		try {
			final var mediaType = MediaType.parseMediaType(contentType);
			// A problem is answered as application/problem+json, and its detail is as free as any other text
			return mediaType.isCompatibleWith(APPLICATION_JSON) || mediaType.getSubtype().endsWith("+json");
		} catch (final Exception e) {
			return false;
		}
	}
}
