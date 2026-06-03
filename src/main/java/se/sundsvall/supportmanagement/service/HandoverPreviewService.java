package se.sundsvall.supportmanagement.service;

import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.handover.HandoverPreview;
import se.sundsvall.supportmanagement.api.model.handover.HandoverPreviewRequest;

import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

/**
 * Builds a side-effect free preview describing how a source errand would be handed over to another namespace.
 *
 * <p>
 * This is currently a stub. The matching logic (auto-suggesting status/classification/label/contactReason targets
 * according to the name → displayName → resourcePath priority rules) together with its repository lookups and
 * integration tests is delivered in a follow-up. The API surface (resource and models) is in place so frontend can
 * integrate against the contract.
 * </p>
 */
@Service
public class HandoverPreviewService {

	/**
	 * Builds a handover preview for the given source errand against the target namespace described in the request.
	 *
	 * @param  namespace      the source namespace
	 * @param  municipalityId the source municipality id
	 * @param  errandId       the source errand id
	 * @param  request        the target namespace/municipality to preview a handover to
	 * @return                a {@link HandoverPreview} describing copyable, mappable and non-copyable fields
	 */
	public HandoverPreview previewHandover(final String namespace, final String municipalityId, final String errandId, final HandoverPreviewRequest request) {
		throw Problem.valueOf(NOT_IMPLEMENTED, "Handover preview is not yet implemented");
	}
}
