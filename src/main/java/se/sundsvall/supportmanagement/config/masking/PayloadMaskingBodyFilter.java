package se.sundsvall.supportmanagement.config.masking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.zalando.logbook.BodyFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Replaces the text a JSON payload carries before it is logged, keeping only the fields
 * {@link PayloadMaskingProperties#keep()} names.
 * <p>
 * Registered as a {@link BodyFilter} bean, which dept44's Logbook configuration collects and applies to every request
 * and response it logs, ours and the ones made to the services around us.
 * <p>
 * The body is parsed once, walked once and written once, whatever the number of fields involved. The JSONPath filters
 * dept44 builds from {@code logbook.body-filters} parse and write the whole body once per path, so masking fifty
 * fields of a small errand cost about fifty times the single pass this does, and a page of a hundred errands most of a
 * second. Both run on the thread that is answering the request.
 * <p>
 * Kept as it is, regardless of the field it belongs to: every property name, so a log line still shows the shape of
 * what was sent, and every number, boolean and null, which carry sizes, counts, versions and flags rather than text.
 * A personal identity number written as a number rather than a string is therefore not caught here - that is what
 * {@code dept44.logback.pii-masking} is under this.
 */
public class PayloadMaskingBodyFilter implements BodyFilter {

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

	/** Field names kept wherever they occur. */
	private final Set<String> keep;

	/** Field names kept only under one parent, by that parent: what {@code metadata.value} in the list means. */
	private final Map<String, Set<String>> keepWithin;

	private final String placeholder;
	private final int maxSize;

	public PayloadMaskingBodyFilter(final PayloadMaskingProperties properties) {
		final var plain = new HashSet<String>();
		final var scoped = new HashMap<String, Set<String>>();
		properties.keep().forEach(entry -> {
			final var separator = entry.indexOf('.');
			if (separator < 0) {
				plain.add(entry);
			} else {
				scoped.computeIfAbsent(entry.substring(0, separator), key -> new HashSet<>())
					.add(entry.substring(separator + 1));
			}
		});
		this.keep = Set.copyOf(plain);
		this.keepWithin = Map.copyOf(scoped);
		this.placeholder = properties.placeholder();
		this.maxSize = properties.maxSize();
	}

	private boolean kept(final String name, final String within) {
		// A string in an array at the root of a body has neither a name nor a parent, and is kept by nothing
		if (isNull(name)) {
			return false;
		}
		return keep.contains(name) || (nonNull(within) && keepWithin.getOrDefault(within, Set.of()).contains(name));
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
			mask(root, null);
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
	 * @param node   the node to mask in place.
	 * @param within the name of the field this node was found under. Two things are judged by it: an element of an
	 *               array, since the strings in {@code recipients} are the value of {@code recipients} and have no name
	 *               of their own, and a field kept only under one parent, which is what {@code metadata.value} in the
	 *               list means.
	 */
	private void mask(final JsonNode node, final String within) {
		if (node instanceof final ObjectNode object) {
			maskProperties(object, within);
		} else if (node instanceof final ArrayNode array) {
			maskElements(array, within);
		}
	}

	private void maskProperties(final ObjectNode object, final String within) {
		// Collected and replaced afterwards rather than as they are found, so that nothing is written to the object
		// while it is being read
		final var replace = new ArrayList<String>();
		object.properties().forEach(property -> {
			final var value = property.getValue();
			if (value.isObject() || value.isArray()) {
				mask(value, property.getKey());
			} else if (isText(value) && !kept(property.getKey(), within)) {
				replace.add(property.getKey());
			}
		});
		replace.forEach(name -> object.put(name, placeholder));
	}

	private void maskElements(final ArrayNode array, final String within) {
		for (var i = 0; i < array.size(); i++) {
			final var element = array.get(i);
			if (element.isObject() || element.isArray()) {
				mask(element, within);
			} else if (isText(element) && !kept(within, null)) {
				array.set(i, placeholder);
			}
		}
	}

	private static boolean isText(final JsonNode node) {
		return node.isString();
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
