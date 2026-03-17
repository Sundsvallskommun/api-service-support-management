package se.sundsvall.supportmanagement.integration.elasticsearch;

import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "json_parameters")
public class JsonParameterDocument {

	@Id
	private String id;

	@Field(type = FieldType.Keyword)
	private String errandId;

	@Field(type = FieldType.Keyword)
	private String namespace;

	@Field(type = FieldType.Keyword)
	private String municipalityId;

	@Field(type = FieldType.Keyword)
	private String parameterKey;

	@Field(type = FieldType.Object)
	private Map<String, Object> value;

	public static JsonParameterDocument create() {
		return new JsonParameterDocument();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public JsonParameterDocument withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public JsonParameterDocument withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public JsonParameterDocument withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public JsonParameterDocument withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getParameterKey() {
		return parameterKey;
	}

	public void setParameterKey(final String parameterKey) {
		this.parameterKey = parameterKey;
	}

	public JsonParameterDocument withParameterKey(final String parameterKey) {
		this.parameterKey = parameterKey;
		return this;
	}

	public Map<String, Object> getValue() {
		return value;
	}

	public void setValue(final Map<String, Object> value) {
		this.value = value;
	}

	public JsonParameterDocument withValue(final Map<String, Object> value) {
		this.value = value;
		return this;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final JsonParameterDocument other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(errandId, other.errandId) && Objects.equals(namespace, other.namespace)
			&& Objects.equals(municipalityId, other.municipalityId) && Objects.equals(parameterKey, other.parameterKey) && Objects.equals(value, other.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, namespace, municipalityId, parameterKey, value);
	}

	@Override
	public String toString() {
		return "JsonParameterDocument{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", namespace='" + namespace + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", parameterKey='" + parameterKey + '\'' +
			", value=" + value +
			'}';
	}
}
