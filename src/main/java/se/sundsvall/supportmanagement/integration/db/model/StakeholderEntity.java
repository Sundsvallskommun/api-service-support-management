package se.sundsvall.supportmanagement.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "stakeholder")
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
	@CollectionTable(name = "contact_channel", joinColumns = @JoinColumn(name = "stakeholder_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_stakeholder_contact_channel_stakeholder_id")))
	private List<ContactChannelEntity> contactChannels;

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

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public StakeholderEntity withId(long id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public StakeholderEntity withErrandEntity(ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public StakeholderEntity withExternalId(String externalId) {
		this.externalId = externalId;
		return this;
	}

	public String getExternalIdType() {
		return externalIdType;
	}

	public void setExternalIdType(String externalIdType) {
		this.externalIdType = externalIdType;
	}

	public StakeholderEntity withExternalIdType(String externalIdType) {
		this.externalIdType = externalIdType;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public StakeholderEntity withRole(String role) {
		this.role = role;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public StakeholderEntity withFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public StakeholderEntity withLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public StakeholderEntity withAddress(String address) {
		this.address = address;
		return this;
	}

	public String getCareOf() {
		return careOf;
	}

	public void setCareOf(String careOf) {
		this.careOf = careOf;
	}

	public StakeholderEntity withCareOf(String careOf) {
		this.careOf = careOf;
		return this;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public StakeholderEntity withZipCode(String zipCode) {
		this.zipCode = zipCode;
		return this;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public StakeholderEntity withCountry(String country) {
		this.country = country;
		return this;
	}

	public List<ContactChannelEntity> getContactChannels() {
		return contactChannels;
	}

	public void setContactChannels(List<ContactChannelEntity> contactChannels) {
		this.contactChannels = contactChannels;
	}

	public StakeholderEntity withContactChannels(List<ContactChannelEntity> contactChannels) {
		this.contactChannels = contactChannels;
		return this;
	}

	@Override
	public String toString() {
		return "StakeholderEntity{" +
			"id=" + id +
			", errandEntity=" + errandEntity +
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
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final StakeholderEntity that = (StakeholderEntity) o;
		return id == that.id && Objects.equals(errandEntity, that.errandEntity) && Objects.equals(externalId, that.externalId) && Objects.equals(externalIdType, that.externalIdType) && Objects.equals(city, that.city) && Objects.equals(organizationName, that.organizationName) && Objects.equals(role, that.role) && Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(address, that.address) && Objects.equals(careOf, that.careOf) && Objects.equals(zipCode, that.zipCode) && Objects.equals(country, that.country) && Objects.equals(contactChannels, that.contactChannels);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandEntity, externalId, externalIdType, city, organizationName, role, firstName, lastName, address, careOf, zipCode, country, contactChannels);
	}
}
