package se.sundsvall.supportmanagement.api.model.revision.deserializer;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class RawDataDeserializer extends JsonDeserializer<String> {
	private static final String UNWANTED_CHARACTERS = "(\\r)|(\\n)";
	private static final String EXCESSIVE_SPACES = " {2,}";

	@Override
	public String deserialize(final JsonParser p, final DeserializationContext context) throws IOException {
		final ObjectMapper mapper = (ObjectMapper) p.getCodec();
		final JsonNode node = mapper.readTree(p);
		// if node is interpreted as a container node the node content is formatted and returned as raw data
		if (node.isContainerNode()) {
			return mapper.writeValueAsString(node)
				.replaceAll(UNWANTED_CHARACTERS, EMPTY)
				.replaceAll(EXCESSIVE_SPACES, SPACE);
		}

		return node.asText();
	}
}
