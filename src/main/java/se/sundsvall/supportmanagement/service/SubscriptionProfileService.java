package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionProfile;
import se.sundsvall.supportmanagement.integration.db.SubscriptionProfileRepository;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionProfileEntity;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.SubscriptionProfileMapper.toSubscriptionProfile;
import static se.sundsvall.supportmanagement.service.mapper.SubscriptionProfileMapper.toSubscriptionProfileEntity;
import static se.sundsvall.supportmanagement.service.mapper.SubscriptionProfileMapper.toSubscriptionProfileList;
import static se.sundsvall.supportmanagement.service.mapper.SubscriptionProfileMapper.updateEntity;

@Service
public class SubscriptionProfileService {

	private static final String PROFILE_NOT_FOUND = "Subscription profile with id:'%s' not found in namespace:'%s' for municipality with id:'%s'";
	private static final String PROFILE_CONFLICT = "Subscription profile with name:'%s' already exists in namespace:'%s' for municipality with id:'%s'";

	private static final String PROFILE_IS_REPORTER_PROFILE = "Subscription profile with id:'%s' is the reporter profile of namespace:'%s' for municipality with id:'%s' and cannot be deleted";

	private final SubscriptionProfileRepository subscriptionProfileRepository;
	private final NamespaceConfigService namespaceConfigService;

	public SubscriptionProfileService(final SubscriptionProfileRepository subscriptionProfileRepository, final NamespaceConfigService namespaceConfigService) {
		this.subscriptionProfileRepository = subscriptionProfileRepository;
		this.namespaceConfigService = namespaceConfigService;
	}

	@Transactional(readOnly = true)
	public List<SubscriptionProfile> findSubscriptionProfiles(final String municipalityId, final String namespace) {
		return toSubscriptionProfileList(subscriptionProfileRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId));
	}

	@Transactional(readOnly = true)
	public SubscriptionProfile findSubscriptionProfile(final String municipalityId, final String namespace, final String profileId) {
		return toSubscriptionProfile(loadEntity(municipalityId, namespace, profileId));
	}

	@Transactional(readOnly = true)
	public SubscriptionProfileEntity findEntity(final String municipalityId, final String namespace, final String profileId) {
		return loadEntity(municipalityId, namespace, profileId);
	}

	/**
	 * Shared by the transactional methods of this service, which call it rather than {@link #findEntity} so that each
	 * runs in the transaction it declares itself.
	 */
	private SubscriptionProfileEntity loadEntity(final String municipalityId, final String namespace, final String profileId) {
		return subscriptionProfileRepository.findByIdAndNamespaceAndMunicipalityId(profileId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, PROFILE_NOT_FOUND.formatted(profileId, namespace, municipalityId)));
	}

	@Transactional
	public String createSubscriptionProfile(final String municipalityId, final String namespace, final SubscriptionProfile profile) {
		if (subscriptionProfileRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, profile.getName())) {
			throw conflict(profile.getName(), namespace, municipalityId);
		}
		return persistOrThrowConflict(toSubscriptionProfileEntity(municipalityId, namespace, profile)).getId();
	}

	@Transactional
	public SubscriptionProfile updateSubscriptionProfile(final String municipalityId, final String namespace, final String profileId, final SubscriptionProfile patch) {
		final var entity = loadEntity(municipalityId, namespace, profileId);
		updateEntity(entity, patch);
		return toSubscriptionProfile(persistOrThrowConflict(entity));
	}

	/**
	 * Removing a profile also removes every subscription to it, through the cascading foreign key on subscription, since
	 * a subscription governed by a profile has nothing left to deliver once the profile is gone.
	 */
	@Transactional
	public void deleteSubscriptionProfile(final String municipalityId, final String namespace, final String profileId) {
		final var entity = loadEntity(municipalityId, namespace, profileId);
		// Reporters would quietly stop being subscribed, so the namespace has to be pointed elsewhere first
		if (namespaceConfigService.isReporterProfile(namespace, municipalityId, profileId)) {
			throw Problem.valueOf(CONFLICT, PROFILE_IS_REPORTER_PROFILE.formatted(profileId, namespace, municipalityId));
		}
		subscriptionProfileRepository.delete(entity);
	}

	// Flush eagerly so the uq_subscription_profile_municipality_namespace_name constraint fires inside this method, both on
	// create (a race past the pre-check) and on update (a rename onto an existing name). Translate to 409 instead of 500.
	private SubscriptionProfileEntity persistOrThrowConflict(final SubscriptionProfileEntity entity) {
		try {
			return subscriptionProfileRepository.saveAndFlush(entity);
		} catch (final DataIntegrityViolationException e) {
			throw conflict(entity.getName(), entity.getNamespace(), entity.getMunicipalityId());
		}
	}

	private static ThrowableProblem conflict(final String name, final String namespace, final String municipalityId) {
		return Problem.valueOf(CONFLICT, PROFILE_CONFLICT.formatted(name, namespace, municipalityId));
	}
}
