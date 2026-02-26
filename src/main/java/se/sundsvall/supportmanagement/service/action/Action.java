package se.sundsvall.supportmanagement.service.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.config.action.Definition;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

public interface Action {

	String getName();

	String getDescription();

	List<Definition> getConditionDefinitions();

	List<Definition> getParameterDefinitions();

	void validateConditions(String municipalityId, String namespace, Map<String, List<String>> conditions) throws ThrowableProblem;

	void validateParameters(String municipalityId, String namespace, Map<String, List<String>> parameters) throws ThrowableProblem;

	boolean actionFulfilled(ErrandEntity errand, Map<String, List<String>> parameters);

	Optional<ErrandActionEntity> createAction(ErrandEntity errand, ActionConfigEntity actionConfigEntity);

	void executeAction(ErrandEntity errand, ActionConfigEntity actionConfigEntity);
}
