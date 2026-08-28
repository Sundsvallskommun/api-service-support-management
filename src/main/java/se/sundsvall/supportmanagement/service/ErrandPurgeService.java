package se.sundsvall.supportmanagement.service;

import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.purge.ErrandPurgeRequest;
import se.sundsvall.supportmanagement.api.model.errand.purge.ErrandPurgeStatus;

import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

/**
 * Entry point for purging errands that have passed their retention period.
 * <p>
 * This class currently carries the contract only. The API surface, its validation and its documentation are delivered
 * ahead of the machinery that does the work, so every operation answers 501 until the purge worker lands. Callers are
 * told plainly that the operation does not exist yet rather than being met with a run that silently does nothing.
 */
@Service
public class ErrandPurgeService {

	private static final String NOT_IMPLEMENTED_MESSAGE = "Errand purge is not implemented yet";

	/**
	 * Starts a purge run for the sent in namespace and municipality.
	 *
	 * @param  namespace      namespace to purge within.
	 * @param  municipalityId id of the municipality to purge within.
	 * @param  request        cutoff and run settings.
	 * @return                the state of the started run.
	 */
	public ErrandPurgeStatus startPurge(final String namespace, final String municipalityId, final ErrandPurgeRequest request) {
		throw Problem.valueOf(NOT_IMPLEMENTED, NOT_IMPLEMENTED_MESSAGE);
	}

	/**
	 * Reads the state of a purge run.
	 *
	 * @param  namespace      namespace the run belongs to.
	 * @param  municipalityId id of the municipality the run belongs to.
	 * @param  jobId          id of the run.
	 * @return                the state of the run.
	 */
	public ErrandPurgeStatus readPurgeStatus(final String namespace, final String municipalityId, final String jobId) {
		throw Problem.valueOf(NOT_IMPLEMENTED, NOT_IMPLEMENTED_MESSAGE);
	}

	/**
	 * Asks a purge run to stop. The run finishes the errand it is on before it does.
	 *
	 * @param  namespace      namespace the run belongs to.
	 * @param  municipalityId id of the municipality the run belongs to.
	 * @param  jobId          id of the run.
	 * @return                the state of the run at the time the stop was requested.
	 */
	public ErrandPurgeStatus stopPurge(final String namespace, final String municipalityId, final String jobId) {
		throw Problem.valueOf(NOT_IMPLEMENTED, NOT_IMPLEMENTED_MESSAGE);
	}
}
