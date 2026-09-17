package se.sundsvall.supportmanagement.service.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.config.action.Definition;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.OperationType;

public interface Action {

	String getName();

	String getDescription();

	List<Definition> getConditionDefinitions(String municipalityId, String namespace);

	List<Definition> getParameterDefinitions(String municipalityId, String namespace);

	void validateConditions(String municipalityId, String namespace, Map<String, List<String>> conditions) throws ThrowableProblem;

	void validateParameters(String municipalityId, String namespace, Map<String, List<String>> parameters) throws ThrowableProblem;

	boolean actionFulfilled(ErrandEntity errand, Map<String, List<String>> parameters);

	Optional<ErrandActionEntity> createAction(ErrandEntity errand, ActionConfigEntity actionConfigEntity);

	boolean conditionsFulfilled(ErrandEntity errand, ActionConfigEntity actionConfigEntity);

	/**
	 * Carries out the action on the errand.
	 *
	 * @param  errand             the errand to act on.
	 * @param  actionConfigEntity the configuration of the action.
	 * @return                    whether the errand itself was changed, which is what decides whether a scheduled run
	 *                            records the change. Sending something about the errand leaves it as it was.
	 */
	boolean executeAction(ErrandEntity errand, ActionConfigEntity actionConfigEntity);

	boolean validForOperationType(OperationType operationType);

	/**
	 * The operations this action runs on. A config may narrow this set and may not widen it, so a client configuring one
	 * has to be able to read it rather than discover it by being refused.
	 */
	Set<OperationType> getValidOperationTypes();
}
