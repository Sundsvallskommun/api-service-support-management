package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "stakeholder",
	indexes = {
		@Index(name = "idx_stakeholder_external_id_role_errand_id", columnList = "external_id, `role`, errand_id")
	})
public class StakeholderEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_errand_stakeholder_errand_id"))
	private ErrandEntity errandEntity;

	@Column(name = "external_id")
	private String externalId;

	@Column(name = "external_id_type")
	private String externalIdType;

	@Column(name = "city")
	private String city;

	@Column(name = "organization_name")
	private String organizationName;

	@Column(name = "role")
	private String role;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "address")
	private String address;

	@Column(name = "care_of")
	private String careOf;

	@Column(name = "zip_code")
	private String zipCode;

	@Column(name = "country")
	private String country;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "contact_channel",
		joinColumns = @JoinColumn(name = "stakeholder_id",
			referencedColumnName = "id",
			foreignKey = @ForeignKey(name = "fk_stakeholder_contact_channel_stakeholder_id")),
		indexes = {
			@Index(
				name = "idx_contact_channel_type_value",
				columnList = "type, value"),
			@Index(
				name = "idx_contact_channel_value",
				columnList = "value")
		})
	private List<ContactChannelEntity> contactChannels;

	@OneToMany(mappedBy = "stakeholderEntity", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<StakeholderParameterEntity> parameters;

	public static StakeholderEntity create() {
		return new StakeholderEntity();
	}

	public String getCity() {
		return city;
	}

	public void setCity(final String city) {
		this.city = city;
	}

	public StakeholderEntity withCity(final String city) {
		this.city = city;
		return this;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public void setOrganizationName(final String organizationName) {
		this.organizationName = organizationName;
	}

	public StakeholderEntity withOrganizationName(final String organizationName) {
		this.organizationName = organizationName;
		return this;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(final String externalId) {
		this.externalId = externalId;
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public StakeholderEntity withId(final long id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public StakeholderEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public StakeholderEntity withExternalId(final String externalId) {
		this.externalId = externalId;
		return this;
	}

	public String getExternalIdType() {
		return externalIdType;
	}

	public void setExternalIdType(final String externalIdType) {
		this.externalIdType = externalIdType;
	}

	public StakeholderEntity withExternalIdType(final String externalIdType) {
		this.externalIdType = externalIdType;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public StakeholderEntity withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public StakeholderEntity withFirstName(final String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public StakeholderEntity withLastName(final String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(final String address) {
		this.address = address;
	}

	public StakeholderEntity withAddress(final String address) {
		this.address = address;
		return this;
	}

	public String getCareOf() {
		return careOf;
	}

	public void setCareOf(final String careOf) {
		this.careOf = careOf;
	}

	public StakeholderEntity withCareOf(final String careOf) {
		this.careOf = careOf;
		return this;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(final String zipCode) {
		this.zipCode = zipCode;
	}

	public StakeholderEntity withZipCode(final String zipCode) {
		this.zipCode = zipCode;
		return this;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(final String country) {
		this.country = country;
	}

	public StakeholderEntity withCountry(final String country) {
		this.country = country;
		return this;
	}

	public List<ContactChannelEntity> getContactChannels() {
		return contactChannels;
	}

	public void setContactChannels(final List<ContactChannelEntity> contactChannels) {
		this.contactChannels = contactChannels;
	}

	public StakeholderEntity withContactChannels(final List<ContactChannelEntity> contactChannels) {
		this.contactChannels = contactChannels;
		return this;
	}

	public List<StakeholderParameterEntity> getParameters() {
		return parameters;
	}

	public void setParameters(final List<StakeholderParameterEntity> parameters) {
		this.parameters = parameters;
	}

	public StakeholderEntity withParameters(final List<StakeholderParameterEntity> parameters) {
		this.parameters = parameters;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, careOf, city, contactChannels, country, errandEntity, externalId, externalIdType, firstName, id, lastName, organizationName, parameters, role, zipCode);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final StakeholderEntity other)) {
			return false;
		}
		return Objects.equals(address, other.address) && Objects.equals(careOf, other.careOf) && Objects.equals(city, other.city) && Objects.equals(contactChannels, other.contactChannels) && Objects.equals(country, other.country) && Objects.equals(
			errandEntity, other.errandEntity) && Objects.equals(externalId, other.externalId) && Objects.equals(externalIdType, other.externalIdType) && Objects.equals(firstName, other.firstName) && (id == other.id) && Objects.equals(lastName,
				other.lastName) && Objects.equals(organizationName, other.organizationName) && Objects.equals(parameters, other.parameters) && Objects.equals(role, other.role) && Objects.equals(zipCode, other.zipCode);
	}

	@Override
	public String toString() {
		return "StakeholderEntity{" +
			"id=" + id +
			", errandEntity=" + (errandEntity != null ? errandEntity.getId() : "null") +
			", externalId='" + externalId + '\'' +
			", externalIdType='" + externalIdType + '\'' +
			", city='" + city + '\'' +
			", organizationName='" + organizationName + '\'' +
			", role='" + role + '\'' +
			", firstName='" + firstName + '\'' +
			", lastName='" + lastName + '\'' +
			", address='" + address + '\'' +
			", careOf='" + careOf + '\'' +
			", zipCode='" + zipCode + '\'' +
			", country='" + country + '\'' +
			", contactChannels=" + contactChannels +
			", parameters=" + parameters +
			'}';

	}
}
