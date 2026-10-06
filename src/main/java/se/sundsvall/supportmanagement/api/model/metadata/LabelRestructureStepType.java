package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The kind of change a label-restructure step makes")
public enum LabelRestructureStepType {

	@Schema(description = "Create a new label under the parent named by path (all but the last segment)")
	ADD,

	@Schema(description = "Change the display name of the label named by path. The id and resourceName are retained")
	RENAME,

	@Schema(description = "Delete the label named by path. It must have no children and no errand may reference it")
	DELETE,

	@Schema(description = "Move the label named by path under destinationParentPath, optionally also renaming it")
	MOVE,

	@Schema(description = "Merge the labels named by sourcePaths into the label named by path, restowing their errands onto it")
	MERGE
}
