package se.sundsvall.supportmanagement.integration.elasticsearch.model;

import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;

@Document(indexName = "errand", dynamic = Dynamic.TRUE)
public class JsonParameterDocument {

	@Id
	@Field(type = Keyword)
	private String id;

	@Field(type = Keyword)
	private String namespace;

	@Field(type = Keyword)
	private String municipalityId;

	@Field(type = FieldType.Object, dynamic = Dynamic.TRUE)
	private Map<String, Object> jsonParameters;

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

	public Map<String, Object> getJsonParameters() {
		return jsonParameters;
	}

	public void setJsonParameters(final Map<String, Object> jsonParameters) {
		this.jsonParameters = jsonParameters;
	}

	public JsonParameterDocument withJsonParameters(final Map<String, Object> jsonParameters) {
		this.jsonParameters = jsonParameters;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, namespace, municipalityId, jsonParameters);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final JsonParameterDocument other)) {
			return false;
		}
		return Objects.equals(id, other.id) && Objects.equals(namespace, other.namespace) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(jsonParameters, other.jsonParameters);
	}

	@Override
	public String toString() {
		return "JsonParameterDocument{" +
			"id='" + id + '\'' +
			", namespace='" + namespace + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", jsonParameters=" + jsonParameters +
			'}';
	}
}
