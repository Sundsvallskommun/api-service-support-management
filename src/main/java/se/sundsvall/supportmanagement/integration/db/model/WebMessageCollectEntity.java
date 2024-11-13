package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "web_message_collect",
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_namespace_municipality_id_instance_family_id",
			columnNames = { "namespace", "municipality_id", "instance" })
	}
)
public class WebMessageCollectEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "municipality_id", nullable = false)
	private String municipalityId;

	@Column(name = "namespace", nullable = false)
	private String namespace;

	@Column(name = "instance", nullable = false)
	private String instance;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "web_message_collect_family_ids",
		joinColumns = @JoinColumn(name = "web_message_collect_id",
			foreignKey = @ForeignKey(name = "fk_web_message_collect_family_ids_web_message_collect_id")))
	@Column(name = "family_id")
	private List<String> familyIds;

	public static WebMessageCollectEntity create() {
		return new WebMessageCollectEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public WebMessageCollectEntity withId(Long id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public WebMessageCollectEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public WebMessageCollectEntity withNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getInstance() {
		return instance;
	}

	public void setInstance(String instance) {
		this.instance = instance;
	}

	public WebMessageCollectEntity withInstance(String instance) {
		this.instance = instance;
		return this;
	}

	public List<String> getFamilyIds() {
		return familyIds;
	}

	public void setFamilyIds(List<String> familyIds) {
		this.familyIds = familyIds;
	}

	public WebMessageCollectEntity withFamilyIds(List<String> familyIds) {
		this.familyIds = familyIds;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		WebMessageCollectEntity that = (WebMessageCollectEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace) && Objects.equals(instance, that.instance) && Objects.equals(
			familyIds, that.familyIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, instance, familyIds);
	}

	@Override
	public String toString() {
		return "WebMessageCollectEntity{" +
			"id=" + id +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", instance='" + instance + '\'' +
			", familyIds='" + familyIds + '\'' +
			'}';
	}
}
