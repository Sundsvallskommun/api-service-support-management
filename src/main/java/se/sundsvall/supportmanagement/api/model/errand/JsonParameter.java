package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;
import tools.jackson.databind.JsonNode;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static se.sundsvall.supportmanagement.Constants.JSON_PARAMETER_KEY_REGEXP;
import static se.sundsvall.supportmanagement.Constants.JSON_PARAMETER_KEY_VALIDATION_MESSAGE;

@Schema(description = "JSON Parameter model")
public class JsonParameter {

	/**
	 * Keys are unique per owner in a column compared without regard to case, and looked up the same way. Held to the
	 * length of that column and to characters every such comparison agrees on, so that no key the lookup tells apart
	 * from a stored one is one the database takes for it.
	 */
	@Schema(description = "Parameter key/name", examples = "formData1")
	@NotBlank
	@Size(min = 1, max = 255)
	@Pattern(regexp = JSON_PARAMETER_KEY_REGEXP, message = JSON_PARAMETER_KEY_VALIDATION_MESSAGE)
	private String key;

	@Schema(description = "JSON structure value", example = """
		{
		  "firstName": "Joe",
		  "lastName": "Doe"
		}
		""")
	@NotNull
	private JsonNode value;

	@Schema(description = "ID referencing a schema in the json-schema service", examples = "2281_person_1.0")
	@NotBlank
	private String schemaId;

	@Schema(description = "Optimistic locking version of the JSON parameter", accessMode = READ_ONLY)
	@Null(groups = {
		OnCreate.class, OnUpdate.class
	})
	private Long version;

	public static JsonParameter create() {
		return new JsonParameter();
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(final Long version) {
		this.version = version;
	}

	public JsonParameter withVersion(final Long version) {
		this.version = version;
		return this;
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
		return Objects.hash(key, schemaId, value, version);
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
		return Objects.equals(key, other.key) && Objects.equals(schemaId, other.schemaId) && Objects.equals(value, other.value) && Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "JsonParameter [key=" + key + ", value=" + value + ", schemaId=" + schemaId + ", version=" + version + "]";
	}
}
