package se.sundsvall.supportmanagement.integration.db.search;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.List;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JsonParametersBinderTest {

	@Mock
	private IndexFieldReference<JsonElement> jsonFieldMock;

	@Mock
	private IndexFieldReference<String> textFieldMock;

	@Mock
	private DocumentElement documentMock;

	private record Parameter(String getKey, String getValue)
		implements
		JsonParameterValue {}

	@Test
	void writesParametersAsOneJsonObjectAndEveryScalarAsText() {
		final var bridge = new JsonParametersBinder.Bridge(jsonFieldMock, textFieldMock);

		bridge.write(documentMock, List.of(
			new Parameter("vehicle", "{\"regNo\":\"ABC123\",\"owner\":{\"name\":\"Anna\"},\"tags\":[\"diesel\",42,true,null]}"),
			new Parameter("note", "\"just a string\"")), null);

		final var json = ArgumentCaptor.forClass(JsonElement.class);
		verify(documentMock).addValue(eq(jsonFieldMock), json.capture());
		assertThat(json.getValue()).isEqualTo(JsonParser.parseString("{\"vehicle\":{\"regNo\":\"ABC123\",\"owner\":{\"name\":\"Anna\"},\"tags\":[\"diesel\",42,true,null]},\"note\":\"just a string\"}"));

		final var texts = ArgumentCaptor.forClass(String.class);
		verify(documentMock, times(6)).addValue(eq(textFieldMock), texts.capture());
		assertThat(texts.getAllValues()).containsExactly("ABC123", "Anna", "diesel", "42", "true", "just a string");
	}

	@Test
	void leavesOutWhatIsNotJsonOrHasNoKey() {
		final var bridge = new JsonParametersBinder.Bridge(jsonFieldMock, textFieldMock);

		bridge.write(documentMock, List.of(
			new Parameter("broken", "{not json"),
			new Parameter(null, "{\"a\":1}"),
			new Parameter("empty", null),
			new Parameter("fine", "{\"a\":1}")), null);

		final var json = ArgumentCaptor.forClass(JsonElement.class);
		verify(documentMock).addValue(eq(jsonFieldMock), json.capture());
		assertThat(json.getValue()).isEqualTo(JsonParser.parseString("{\"fine\":{\"a\":1}}"));
		verify(documentMock).addValue(textFieldMock, "1");
	}

	@Test
	void writesNothingWithoutParameters() {
		final var bridge = new JsonParametersBinder.Bridge(jsonFieldMock, textFieldMock);

		bridge.write(documentMock, null, null);
		bridge.write(documentMock, List.of(), null);
		bridge.write(documentMock, List.of(new Parameter("broken", "{not json")), null);

		verify(documentMock, never()).addValue(eq(jsonFieldMock), any(JsonElement.class));
		verifyNoInteractions(jsonFieldMock, textFieldMock);
	}
}
