package se.sundsvall.supportmanagement.service.action;

import java.util.List;
import java.util.Map;
import org.zalando.problem.ThrowableProblem;

public interface Action {

	String getName();

	void validateConditions(String municipalityId, String namespace, Map<String, List<String>> conditions) throws ThrowableProblem;

	void validateParameters(String municipalityId, String namespace, Map<String, List<String>> parameters) throws ThrowableProblem;
}
