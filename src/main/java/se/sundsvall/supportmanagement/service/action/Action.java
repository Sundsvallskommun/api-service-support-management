package se.sundsvall.supportmanagement.service.action;

import java.util.List;
import java.util.Map;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.config.action.Definition;

public interface Action {

	String getName();

	String getDescription();

	List<Definition> getConditionDefinitions();

	List<Definition> getParameterDefinitions();

	void validateConditions(String municipalityId, String namespace, Map<String, List<String>> conditions) throws ThrowableProblem;

	void validateParameters(String municipalityId, String namespace, Map<String, List<String>> parameters) throws ThrowableProblem;
}
