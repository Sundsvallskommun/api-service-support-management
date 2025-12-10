package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.validation.ValidRole;

@Schema(description = "Stakeholder model")
public class Stakeholder {

	@Schema(description = "Unique identifier for the stakeholder", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	private String externalId;

	@Schema(description = "Type of external id", examples = "PRIVATE")
	private String externalIdType;

	@Schema(description = "Role of stakeholder", examples = "ADMINISTRATOR")
	@ValidRole
	private String role;

	@Schema(description = "City", examples = "Cottington")
	private String city;

	@Schema(description = "Organization name", examples = "Vogon Constructor Fleet")
	private String organizationName;

	@Schema(description = "First name", examples = "Aurthur")
	private String firstName;

	@Schema(description = "Last name", examples = "Dent")
	private String lastName;

	@Schema(description = "Address", examples = "155 Country Lane, Cottington")
	private String address;

	@Schema(description = "Care of", examples = "Ford Prefect")
	private String careOf;

	@Schema(description = "Zip code", examples = "12345")
	private String zipCode;

	@Schema(description = "Country", examples = "United Kingdom")
	private String country;

	@ArraySchema(schema = @Schema(implementation = ContactChannel.class))
	private List<ContactChannel> contactChannels;

	@Schema(description = "Parameters for the stakeholder")
	private List<@Valid Parameter> parameters;

	public static Stakeholder create() {
		return new Stakeholder();
	}

	public String getCity() {
		return city;
	}

	public void setCity(final String city) {
		this.city = city;
	}

	public Stakeholder withCity(final String city) {
		this.city = city;
		return this;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public void setOrganizationName(final String organizationName) {
		this.organizationName = organizationName;
	}

	public Stakeholder withOrganizationName(final String organizationName) {
		this.organizationName = organizationName;
		return this;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(final String externalId) {
		this.externalId = externalId;
	}

	public Stakeholder withExternalId(final String externalId) {
		this.externalId = externalId;
		return this;
	}

	public String getExternalIdType() {
		return externalIdType;
	}

	public void setExternalIdType(final String externalIdType) {
		this.externalIdType = externalIdType;
	}

	public Stakeholder withExternalIdType(final String externalIdType) {
		this.externalIdType = externalIdType;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public Stakeholder withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public Stakeholder withFirstName(final String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public Stakeholder withLastName(final String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(final String address) {
		this.address = address;
	}

	public Stakeholder withAddress(final String address) {
		this.address = address;
		return this;
	}

	public String getCareOf() {
		return careOf;
	}

	public void setCareOf(final String careOf) {
		this.careOf = careOf;
	}

	public Stakeholder withCareOf(final String careOf) {
		this.careOf = careOf;
		return this;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(final String zipCode) {
		this.zipCode = zipCode;
	}

	public Stakeholder withZipCode(final String zipCode) {
		this.zipCode = zipCode;
		return this;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(final String country) {
		this.country = country;
	}

	public Stakeholder withCountry(final String country) {
		this.country = country;
		return this;
	}

	public List<ContactChannel> getContactChannels() {
		return contactChannels;
	}

	public void setContactChannels(final List<ContactChannel> contactChannels) {
		this.contactChannels = contactChannels;
	}

	public Stakeholder withContactChannels(final List<ContactChannel> contactChannels) {
		this.contactChannels = contactChannels;
		return this;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(final List<Parameter> parameters) {
		this.parameters = parameters;
	}

	public Stakeholder withParameters(final List<Parameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, careOf, city, contactChannels, country, externalId, externalIdType, firstName, lastName, organizationName, parameters, role, zipCode);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Stakeholder other)) {
			return false;
		}
		return Objects.equals(address, other.address) && Objects.equals(careOf, other.careOf) && Objects.equals(city, other.city) && Objects.equals(contactChannels, other.contactChannels) && Objects.equals(country, other.country) && Objects.equals(
			externalId, other.externalId) && Objects.equals(externalIdType, other.externalIdType) && Objects.equals(firstName, other.firstName) && Objects.equals(lastName, other.lastName) && Objects.equals(organizationName, other.organizationName)
			&& Objects.equals(parameters, other.parameters) && Objects.equals(role, other.role) && Objects.equals(zipCode, other.zipCode);
	}

	@Override
	public String toString() {
		return "Stakeholder{" +
			"externalId='" + externalId + '\'' +
			", externalIdType='" + externalIdType + '\'' +
			", role='" + role + '\'' +
			", city='" + city + '\'' +
			", organizationName='" + organizationName + '\'' +
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
