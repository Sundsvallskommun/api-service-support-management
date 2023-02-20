package se.sundsvall.supportmanagement.integration.db;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@Transactional
public interface ErrandsRepository extends CrudRepository<ErrandEntity, String>, JpaRepository<ErrandEntity, String>, JpaSpecificationExecutor<ErrandEntity> {

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);
}
