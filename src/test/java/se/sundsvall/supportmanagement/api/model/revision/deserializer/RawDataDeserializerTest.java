package se.sundsvall.supportmanagement.api.model.revision.deserializer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RawDataDeserializerTest {

	private final RawDataDeserializer deserializer = new RawDataDeserializer();
	@Mock
	private JsonParser jsonParserMock;
	@Mock
	private JsonNode jsonNodeMock;
	@Mock
	private ObjectReadContext objectReadContextMock;

	@ParameterizedTest
	@ValueSource(strings = {
		"{\r\n    \"key-1\"  :  \"value-1\", \r\n    \"key-2\" : \"value-2\"}",
		"{  \n    \"key-1\"  :  \"value-1\",   \n    \"key-2\" : \"value-2\"}"
	})
	void testDeserializingContainerNode(final String testString) throws Exception {
		final var wantedString = "{ \"key-1\" : \"value-1\", \"key-2\" : \"value-2\"}";

		doReturn(objectReadContextMock).when(jsonParserMock).objectReadContext();
		when(objectReadContextMock.readTree(jsonParserMock)).thenReturn(jsonNodeMock);
		when(jsonNodeMock.isContainer()).thenReturn(true);
		when(jsonNodeMock.toString()).thenReturn(testString);

		assertThat(deserializer.deserialize(jsonParserMock, null)).isEqualTo(wantedString);

		verify(jsonNodeMock).isContainer();
		verify(jsonNodeMock, never()).asString();
	}

	@Test
	void testDeserializingNonContainerNode() throws Exception {
		final var wantedString = "someRandomTextString";

		doReturn(objectReadContextMock).when(jsonParserMock).objectReadContext();
		when(objectReadContextMock.readTree(jsonParserMock)).thenReturn(jsonNodeMock);
		when(jsonNodeMock.asString()).thenReturn(wantedString);

		assertThat(deserializer.deserialize(jsonParserMock, null)).isEqualTo(wantedString);

		verify(jsonNodeMock).isContainer();
		verify(jsonNodeMock).asString();

	}

}
