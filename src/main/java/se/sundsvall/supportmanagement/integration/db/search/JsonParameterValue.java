package se.sundsvall.supportmanagement.integration.db.search;

/**
 * What the search index needs from a JSON parameter, whichever entity holds it. The errand and each of its handling
 * artefacts keep their JSON parameters in tables of their own, and this is what lets one binder index all of them.
 */
public interface JsonParameterValue {

	/** The key the parameter is known by on its owner, which becomes the first segment of the indexed field path. */
	String getKey();

	/** The JSON document as stored. */
	String getValue();
}
