package se.sundsvall.supportmanagement.integration.db.model;

import java.util.List;

/**
 * A parameter of a handling artefact: a key with a list of values, a display name and a group, held as unstructured
 * metadata of the artefact.
 * <p>
 * Implemented by {@link DecisionParameterEntity} and {@link InvestigationParameterEntity}, which share this shape and
 * keep it in a table each, beside their owner. This is what lets one mapper map both.
 */
public interface ArtefactParameter {

	String getKey();

	void setKey(String key);

	String getDisplayName();

	void setDisplayName(String displayName);

	String getParameterGroup();

	void setParameterGroup(String parameterGroup);

	List<String> getValues();

	void setValues(List<String> values);
}
