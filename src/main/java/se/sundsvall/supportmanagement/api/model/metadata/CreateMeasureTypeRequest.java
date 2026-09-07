package se.sundsvall.supportmanagement.api.model.metadata;

import io.swagger.v3.oas.annotations.media.Schema;

/** The complete input required when adding a measure type, as distinct from a partial update. */
@Schema(description = "Create a measure type. The name is the immutable key measures refer to", requiredProperties = {
	"name", "measureGroup"
})
public class CreateMeasureTypeRequest extends MeasureType {
}
