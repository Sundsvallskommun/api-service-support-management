package se.sundsvall.supportmanagement.integration.db.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "errand_number_sequence",
	indexes = {
		@Index(name = "idx_errand_number_sequence_namespace_municipality_id", columnList = "namespace, municipality_id"),
	})
public class ErrandNumberSequenceEntity {

	@Id
	@Column(name = "namespace")
	private String namespace;

	@Column(name = "municipality_id")
	private String municipalityId;

	@Column(name = "last_sequence_number")
	private int lastSequenceNumber;

	@Column(name = "reset_year_month", length = 6)
	private String resetYearMonth;

	public static ErrandNumberSequenceEntity create() {
		return new ErrandNumberSequenceEntity();
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public ErrandNumberSequenceEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ErrandNumberSequenceEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public int getLastSequenceNumber() {
		return lastSequenceNumber;
	}

	public void setLastSequenceNumber(final int sequence) {
		this.lastSequenceNumber = sequence;
	}

	public ErrandNumberSequenceEntity withLastSequenceNumber(final int lastSequenceNumber) {
		this.lastSequenceNumber = lastSequenceNumber;
		return this;
	}

	public String getResetYearMonth() {
		return resetYearMonth;
	}

	public void setResetYearMonth(final String resetMonth) {
		this.resetYearMonth = resetMonth;
	}

	public ErrandNumberSequenceEntity withResetYearMonth(final String resetYearMonth) {
		this.resetYearMonth = resetYearMonth;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final ErrandNumberSequenceEntity entity = (ErrandNumberSequenceEntity) o;
		return lastSequenceNumber == entity.lastSequenceNumber
			&& Objects.equals(namespace, entity.namespace)
			&& Objects.equals(municipalityId, entity.municipalityId)
			&& Objects.equals(resetYearMonth, entity.resetYearMonth);
	}

	@Override
	public int hashCode() {
		return Objects.hash(namespace, municipalityId, lastSequenceNumber, resetYearMonth);
	}

	@Override
	public String toString() {
		return "ErrandNumberSequenceEntity{" +
			"namespace=" + namespace +
			", municipalityId=" + municipalityId +
			", lastSequenceNumber=" + lastSequenceNumber +
			", resetYearMonth='" + resetYearMonth + '\'' +
			'}';
	}

}
