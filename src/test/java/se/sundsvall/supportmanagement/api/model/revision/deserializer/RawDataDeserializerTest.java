package se.sundsvall.supportmanagement.api.model.revision.deserializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RawDataDeserializerTest {

	@Mock
	private JsonParser jsonParserMock;

	@Mock
	private ObjectMapper objectMapperMock;

	@Mock
	private JsonNode jsonNodeMock;

	private RawDataDeserializer deserializer = new RawDataDeserializer();

	@Test
	void testDeserializingContainerNode() throws Exception {
		final var testString = "{\r\n    \"key-1\"  :  \"value-1\", \r\n    \"key-2\" : \"value-2\"}";
		final var wantedString = "{ \"key-1\" : \"value-1\", \"key-2\" : \"value-2\"}";

		when(jsonParserMock.getCodec()).thenReturn(objectMapperMock);
		when(objectMapperMock.readTree(jsonParserMock)).thenReturn(jsonNodeMock);
		when(jsonNodeMock.isContainerNode()).thenReturn(true);
		when(objectMapperMock.writeValueAsString(jsonNodeMock)).thenReturn(testString);

		assertThat(deserializer.deserialize(jsonParserMock, null)).isEqualTo(wantedString);

		verify(jsonNodeMock).isContainerNode();
		verify(objectMapperMock).writeValueAsString(jsonNodeMock);
		verify(jsonNodeMock, never()).asText();
	}

	@Test
	void testDeserializingNonContainerNode() throws Exception {
		final var wantedString = "someRandomTextString";

		when(jsonParserMock.getCodec()).thenReturn(objectMapperMock);
		when(objectMapperMock.readTree(jsonParserMock)).thenReturn(jsonNodeMock);
		when(jsonNodeMock.asText()).thenReturn(wantedString);

		assertThat(deserializer.deserialize(jsonParserMock, null)).isEqualTo(wantedString);

		verify(jsonNodeMock).isContainerNode();
		verify(objectMapperMock, never()).writeValueAsString(jsonNodeMock);
		verify(jsonNodeMock).asText();

	}

}
