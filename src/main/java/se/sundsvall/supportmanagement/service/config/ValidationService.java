package se.sundsvall.supportmanagement.service.config;

import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.Validation;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.ValidationRepository;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.mapper.ValidationMapper;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ValidationService {

	private static final String CONFIG_ENTITY_NOT_FOUND = "No config found in namespace '%s' for municipality '%s'";

	private final ValidationRepository validationRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;
	private final ValidationMapper mapper;

	public ValidationService(final ValidationRepository validationRepository, final NamespaceConfigRepository namespaceConfigRepository, final ValidationMapper mapper) {
		this.validationRepository = validationRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.mapper = mapper;
	}

	public List<Validation> findAll(final String namespace, final String municipalityId) {
		verifyNamespaceConfigExists(namespace, municipalityId);

		return mapper.toValidations(validationRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId));
	}

	/**
	 * Turns validation on or off for the sent in type. As a type without a stored validation is treated as not validated,
	 * the validation is created if it does not exist yet.
	 */
	public Validation update(final String namespace, final String municipalityId, final EntityType type, final Validation request) {
		verifyNamespaceConfigExists(namespace, municipalityId);

		final var entity = validationRepository.findByNamespaceAndMunicipalityIdAndType(namespace, municipalityId, type)
			.orElseGet(() -> mapper.toEntity(namespace, municipalityId, type, request.getValidated()))
			.withValidated(request.getValidated());

		return mapper.toValidation(validationRepository.save(entity));
	}

	private void verifyNamespaceConfigExists(final String namespace, final String municipalityId) {
		if (!namespaceConfigRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, CONFIG_ENTITY_NOT_FOUND.formatted(namespace, municipalityId));
		}
	}
}
