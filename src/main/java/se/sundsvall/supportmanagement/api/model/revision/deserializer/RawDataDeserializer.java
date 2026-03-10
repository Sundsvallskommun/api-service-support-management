package se.sundsvall.supportmanagement.api.model.revision.deserializer;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

public class RawDataDeserializer extends ValueDeserializer<String> {
	private static final String UNWANTED_CHARACTERS = "(\\r)|(\\n)";
	private static final String EXCESSIVE_SPACES = " {2,}";

	@Override
	public String deserialize(final JsonParser p, final DeserializationContext context) {
		final var readContext = p.objectReadContext();
		final JsonNode node = readContext.readTree(p);
		// if node is interpreted as a container node the node content is formatted and returned as raw data
		if (node.isContainer()) {
			final var json = node.toString()
				.replace(":", " : ")
				.replace(",", ", ")
				.replace("{", "{ ");
			return json
				.replaceAll(UNWANTED_CHARACTERS, EMPTY)
				.replaceAll(EXCESSIVE_SPACES, SPACE);
		}

		return node.asString();
	}
}
