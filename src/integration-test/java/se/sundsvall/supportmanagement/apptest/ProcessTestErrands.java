package se.sundsvall.supportmanagement.apptest;

import java.util.UUID;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

/**
 * The errand the process integration tests hang their rows on.
 * <p>
 * Shared rather than copied, since a new required column on {@link ErrandEntity} would otherwise have to be found in two
 * places, and two fixtures drifting apart would leave the process tests running against differently shaped errands
 * without anything failing to say so.
 */
final class ProcessTestErrands {

	private ProcessTestErrands() {}

	static String createErrand(final ErrandsRepository repository, final String municipalityId, final String namespace, final String errandNumberPrefix) {
		return repository.saveAndFlush(ErrandEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withErrandNumber(errandNumberPrefix + "-" + UUID.randomUUID())
			.withTitle("TITLE")
			.withStatus("STATUS")
			.withPriority("MEDIUM")
			.withReporterUserId("joe01doe")).getId();
	}
}
