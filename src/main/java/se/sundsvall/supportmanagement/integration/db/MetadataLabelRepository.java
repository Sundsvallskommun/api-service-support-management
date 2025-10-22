package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

@Transactional
@CircuitBreaker(name = "metadataLabelRepository")
public interface MetadataLabelRepository extends JpaRepository<MetadataLabelEntity, String> {

	List<MetadataLabelEntity> findByNamespaceAndMunicipalityIdAndParentIsNull(String namespace, String municipalityId);

	List<MetadataLabelEntity> findByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	Optional<MetadataLabelEntity> findByNamespaceAndMunicipalityIdAndResourcePath(String namespace, String municipalityId, String resourcePath);

	Optional<MetadataLabelEntity> findByNamespaceAndMunicipalityIdAndId(String namespace, String municipalityId, String id);

	boolean existsByNamespaceAndMunicipalityIdAndId(String namespace, String municipalityId, String id);

	boolean existsByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}
