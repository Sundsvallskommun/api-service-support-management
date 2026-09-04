package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collection;
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

	boolean existsByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	List<MetadataLabelEntity> findByNamespaceAndMunicipalityIdAndResourcePathIn(String namespace, String municipalityId, Collection<String> resourcePaths);

	Optional<MetadataLabelEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	List<MetadataLabelEntity> findByNamespaceAndMunicipalityIdAndResourcePathStartingWith(String namespace, String municipalityId, String resourcePathPrefix);
}
