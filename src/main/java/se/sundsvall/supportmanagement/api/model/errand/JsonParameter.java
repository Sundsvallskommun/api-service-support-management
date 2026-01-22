package se.sundsvall.supportmanagement.api.model.errand;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

@Schema(description = "JSON Parameter model")
public class JsonParameter {

	@Schema(description = "Parameter key/name", examples = "formData1")
	@NotBlank
	private String key;

	@Schema(description = "JSON structure value")
	@NotNull
	private JsonNode value;

	@Schema(description = "ID referencing a schema in the json-schema service", examples = "2281_person_1.0")
	@NotBlank
	private String schemaId;

	public static JsonParameter create() {
		return new JsonParameter();
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public JsonParameter withKey(final String key) {
		this.key = key;
		return this;
	}

	public JsonNode getValue() {
		return value;
	}

	public void setValue(final JsonNode value) {
		this.value = value;
	}

	public JsonParameter withValue(final JsonNode value) {
		this.value = value;
		return this;
	}

	public String getSchemaId() {
		return schemaId;
	}

	public void setSchemaId(final String schemaId) {
		this.schemaId = schemaId;
	}

	public JsonParameter withSchemaId(final String schemaId) {
		this.schemaId = schemaId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, schemaId, value);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (JsonParameter) obj;
		return Objects.equals(key, other.key) && Objects.equals(schemaId, other.schemaId) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "JsonParameter [key=" + key + ", value=" + value + ", schemaId=" + schemaId + "]";
	}
}
