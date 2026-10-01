package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.metadata.LabelClassification;
import se.sundsvall.supportmanagement.integration.db.LabelClassificationRepository;
import se.sundsvall.supportmanagement.integration.db.model.LabelClassificationEntity;
import se.sundsvall.supportmanagement.service.mapper.LabelClassificationMapper;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.LabelClassificationMapper.toLabelClassification;
import static se.sundsvall.supportmanagement.service.mapper.LabelClassificationMapper.toLabelClassificationEntity;
import static se.sundsvall.supportmanagement.service.mapper.LabelClassificationMapper.updateLabelClassificationEntity;

/**
 * Display names for the label classifications of a namespace. The display names are resolved onto every label read,
 * of the metadata as well as of the errands, which is why they are cached per namespace and evicted on every change.
 */
@Service
public class LabelClassificationService {

	private static final String CACHE_NAME = "labelClassificationCache";
	private static final String ALREADY_EXISTS = "Label classification '%s' already exists in namespace '%s' for municipalityId '%s'";
	private static final String NOT_PRESENT = "Label classification '%s' is not present in namespace '%s' for municipalityId '%s'";
	private static final Sort DEFAULT_SORT = Sort.by("classification");

	private final LabelClassificationRepository repository;

	public LabelClassificationService(final LabelClassificationRepository repository) {
		this.repository = repository;
	}

	@CacheEvict(value = CACHE_NAME, key = "{#namespace, #municipalityId}")
	public String createLabelClassification(final String namespace, final String municipalityId, final LabelClassification labelClassification) {
		if (repository.existsByNamespaceAndMunicipalityIdAndClassification(namespace, municipalityId, labelClassification.getClassification())) {
			throw Problem.valueOf(BAD_REQUEST, ALREADY_EXISTS.formatted(labelClassification.getClassification(), namespace, municipalityId));
		}

		return repository.save(toLabelClassificationEntity(namespace, municipalityId, labelClassification)).getClassification();
	}

	public LabelClassification getLabelClassification(final String namespace, final String municipalityId, final String classification) {
		return toLabelClassification(getEntity(namespace, municipalityId, classification));
	}

	public List<LabelClassification> findLabelClassifications(final String namespace, final String municipalityId, final Sort sort) {
		return repository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, Objects.isNull(sort) || sort.isUnsorted() ? DEFAULT_SORT : sort)
			.stream()
			.map(LabelClassificationMapper::toLabelClassification)
			.toList();
	}

	@CacheEvict(value = CACHE_NAME, key = "{#namespace, #municipalityId}")
	public LabelClassification updateLabelClassification(final String namespace, final String municipalityId, final String classification, final LabelClassification labelClassification) {
		final var entity = updateLabelClassificationEntity(getEntity(namespace, municipalityId, classification), labelClassification);
		return toLabelClassification(repository.save(entity));
	}

	@Transactional
	@CacheEvict(value = CACHE_NAME, key = "{#namespace, #municipalityId}")
	public void deleteLabelClassification(final String namespace, final String municipalityId, final String classification) {
		if (!repository.existsByNamespaceAndMunicipalityIdAndClassification(namespace, municipalityId, classification)) {
			throw Problem.valueOf(NOT_FOUND, NOT_PRESENT.formatted(classification, namespace, municipalityId));
		}

		repository.deleteByNamespaceAndMunicipalityIdAndClassification(namespace, municipalityId, classification);
	}

	/**
	 * The display names of the label classifications in the namespace, keyed on classification. Classifications without a
	 * display name are left out, so that a lookup of one answers the same as a lookup of a classification never registered.
	 */
	@Cacheable(value = CACHE_NAME, key = "{#namespace, #municipalityId}")
	public Map<String, String> getClassificationDisplayNames(final String namespace, final String municipalityId) {
		return repository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId, Sort.unsorted())
			.stream()
			.filter(entity -> Objects.nonNull(entity.getDisplayName()))
			.collect(toUnmodifiableMap(LabelClassificationEntity::getClassification, LabelClassificationEntity::getDisplayName));
	}

	private LabelClassificationEntity getEntity(final String namespace, final String municipalityId, final String classification) {
		return repository.findByNamespaceAndMunicipalityIdAndClassification(namespace, municipalityId, classification)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOT_PRESENT.formatted(classification, namespace, municipalityId)));
	}
}
