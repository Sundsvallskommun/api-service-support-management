package se.sundsvall.supportmanagement.integration.db.model;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "stakeholder")
public class StakeholderEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 8197399712706968439L;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "errand_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_errand_stakeholder_errand_id"))
    private ErrandEntity errandEntity;
    @Column(name = "stakeholder_id")
    private String stakeholderId;
    @Column(name = "type")
    private String type;

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
    @CollectionTable(name = "contact_channel",
            joinColumns = @JoinColumn(name = "stakeholder_id",
                    referencedColumnName = "id",
                    foreignKey = @ForeignKey(name = "fk_stakeholder_contact_channel_stakeholder_id")
            )
    )
    private List<ContactChannelEntity> contactChannels;

    public static StakeholderEntity create() {
        return new StakeholderEntity();
    }

    public String getStakeholderId() {
        return stakeholderId;
    }

    public void setStakeholderId(String stakeholderId) {
        this.stakeholderId = stakeholderId;
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

    public StakeholderEntity withStakeholderId(String id) {
        this.stakeholderId = id;
        return this;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public StakeholderEntity withType(String type) {
        this.type = type;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StakeholderEntity that = (StakeholderEntity) o;
        return id == that.id && Objects.equals(errandEntity, that.errandEntity) && Objects.equals(stakeholderId, that.stakeholderId) && Objects.equals(type, that.type) && Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(address, that.address) && Objects.equals(careOf, that.careOf) && Objects.equals(zipCode, that.zipCode) && Objects.equals(country, that.country) && Objects.equals(contactChannels, that.contactChannels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, errandEntity, stakeholderId, type, firstName, lastName, address, careOf, zipCode, country, contactChannels);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StakeholderEntity{");
        sb.append("id=").append(id);
        sb.append(", errandEntityId=").append(Optional.ofNullable(errandEntity).map(ErrandEntity::getId).orElse(null));
        sb.append(", stakeholderId='").append(stakeholderId).append('\'');
        sb.append(", type='").append(type).append('\'');
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
