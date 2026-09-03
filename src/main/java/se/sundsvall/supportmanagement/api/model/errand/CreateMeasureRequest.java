package se.sundsvall.supportmanagement.api.model.errand;

import io.swagger.v3.oas.annotations.media.Schema;

/** The complete input required when adding a measure, as distinct from a partial update. */
@Schema(description = "Create a measure. The type and role must exist in the errand namespace", requiredProperties = {
	"type", "addedByUser", "addedByRole"
})
public class CreateMeasureRequest extends Measure {
}
