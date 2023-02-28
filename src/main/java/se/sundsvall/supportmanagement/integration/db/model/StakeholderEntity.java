package se.sundsvall.supportmanagement.integration.db.model;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

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
    @JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_errand_stakeholder_errand_id"))
    private ErrandEntity errandEntity;
    @Column(name = "stakeholder_id")
    private String stakeholderId;
    @Column(name = "type")
    private String type;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StakeholderEntity that = (StakeholderEntity) o;
        return id == that.id && Objects.equals(errandEntity, that.errandEntity) && Objects.equals(stakeholderId, that.stakeholderId) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, errandEntity, stakeholderId, type);
    }

    @Override
    public String
    toString() {
        final StringBuilder sb = new StringBuilder("StakeholderEntity{");
        sb.append("id=").append(id);
        sb.append(", errandEntity=").append(errandEntity);
        sb.append(", stakeholderId='").append(stakeholderId).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
