package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Schema(description = "Stakeholder model")
public class Stakeholder {

	@Schema(description = "Unique identifier for the stakeholder", example = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	private String externalId;

	@Schema(description = "Type of external id", example = "PRIVATE")
	private String externalIdType;

	@Schema(description = "Role of stakeholder", example = "ADMINISTRATOR")
	private String role;

	@Schema(description = "First name", example = "Aurthur")
	private String firstName;

	@Schema(description = "Last name", example = "Dent")
	private String lastName;

	@Schema(description = "Address", example = "155 Country Lane, Cottington")
	private String address;

	@Schema(description = "Care of", example = "Ford Prefect")
	private String careOf;

	@Schema(description = "Zip code", example = "12345")
	private String zipCode;

	@Schema(description = "Country", example = "United Kingdom")
	private String country;

	@ArraySchema(schema = @Schema(implementation = ContactChannel.class))
	private List<ContactChannel> contactChannels;

	public static Stakeholder create() {
		return new Stakeholder();
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public Stakeholder withExternalId(String externalId) {
		this.externalId = externalId;
		return this;
	}

	public String getExternalIdType() {
		return externalIdType;
	}

	public void setExternalIdType(String externalIdType) {
		this.externalIdType = externalIdType;
	}

	public Stakeholder withExternalIdType(String externalIdType) {
		this.externalIdType = externalIdType;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Stakeholder withRole(String role) {
		this.role = role;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public Stakeholder withFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Stakeholder withLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Stakeholder withAddress(String address) {
		this.address = address;
		return this;
	}

	public String getCareOf() {
		return careOf;
	}

	public void setCareOf(String careOf) {
		this.careOf = careOf;
	}

	public Stakeholder withCareOf(String careOf) {
		this.careOf = careOf;
		return this;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public Stakeholder withZipCode(String zipCode) {
		this.zipCode = zipCode;
		return this;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public Stakeholder withCountry(String country) {
		this.country = country;
		return this;
	}

	public List<ContactChannel> getContactChannels() {
		return contactChannels;
	}

	public void setContactChannels(List<ContactChannel> contactChannels) {
		this.contactChannels = contactChannels;
	}

	public Stakeholder withContactChannels(List<ContactChannel> contactChannels) {
		this.contactChannels = contactChannels;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Stakeholder that = (Stakeholder) o;
		return Objects.equals(externalId, that.externalId) && Objects.equals(externalIdType, that.externalIdTypeTag) && Objects.equals(role, that.role) && Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(address, that.address) && Objects.equals(careOf, that.careOf) && Objects.equals(zipCode, that.zipCode) && Objects.equals(country, that.country) && Objects.equals(contactChannels, that.contactChannels);
	}

	@Override
	public int hashCode() {
		return Objects.hash(externalId, externalIdType, role, firstName, lastName, address, careOf, zipCode, country, contactChannels);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Stakeholder{");
		sb.append("externalId='").append(externalId).append('\'');
		sb.append(", externalIdType='").append(externalIdType).append('\'');
		sb.append(", role='").append(role).append('\'');
		sb.append(", firstName='").append(firstName).append('\'');
		sb.append(", lastName='").append(lastName).append('\'');
		sb.append(", address='").append(address).append('\'');
		sb.append(", careOf='").append(careOf).append('\'');
		sb.append(", zipCode='").append(zipCode).append('\'');
		sb.append(", country='").append(country).append('\'');
		sb.append(", contactChannels=").append(contactChannels);
		sb.append('}');
		return sb.toString();
	}
}
