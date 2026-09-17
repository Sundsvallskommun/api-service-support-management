package se.sundsvall.supportmanagement.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.access.ErrandAccess;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.mapper.ErrandAccessMapper;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;

@Service
public class ErrandAccessService {

	private final AccessControlService accessControlService;

	ErrandAccessService(final AccessControlService accessControlService) {
		this.accessControlService = accessControlService;
	}

	/**
	 * Reports what the requesting user may do with sent in errand.
	 * <p>
	 * Guarded on the errand itself at limited read, which is what every plain read of an errand asks for, rather than on
	 * a resource of its own. A resource of its own would have to be granted before the endpoint answered at all, leaving
	 * every namespace already configured unable to ask what its users may do.
	 */
	@Transactional(readOnly = true)
	public ErrandAccess readErrandAccess(final String namespace, final String municipalityId, final String errandId) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, ProtectedResource.ERRAND, LR);

		return ErrandAccessMapper.toErrandAccess(accessControlService.resolveErrandAccess(namespace, municipalityId, Identifier.get(), errandEntity));
	}
}
