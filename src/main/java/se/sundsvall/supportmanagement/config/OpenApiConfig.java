package se.sundsvall.supportmanagement.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import se.sundsvall.supportmanagement.api.model.errand.CreateMeasureRequest;
import se.sundsvall.supportmanagement.api.model.errand.Measure;

import static se.sundsvall.supportmanagement.service.util.ServiceUtil.REQUEST_GROUP_ID_HEADER;

@Configuration
class OpenApiConfig {

	@Bean
	OperationCustomizer requestGroupIdHeaderCustomizer() {
		return (Operation operation, @SuppressWarnings("unused") HandlerMethod handlerMethod) -> {
			operation.addParametersItem(new Parameter()
				.name(REQUEST_GROUP_ID_HEADER)
				.in("header")
				.required(false)
				.description("Optional UUID that groups related events and notifications for this operation. If omitted, no grouping is applied.")
				.example("f47ac10b-58cc-4372-a567-0e02b2c3d479")
				.schema(new StringSchema().format("uuid")));
			return operation;
		};
	}

	@Bean
	OpenApiCustomizer measureDecisionSchemaCustomizer() {
		return openApi -> {
			// In OpenAPI 3.1, allowing the null type is not enough: enum must also contain null.
			// Swagger's nullable enum annotation currently emits only the non-null enum values.
			for (final var model : List.of(Measure.class, CreateMeasureRequest.class)) {
				final Schema<?> schema = openApi.getComponents().getSchemas().get(model.getSimpleName());
				if (schema != null) {
					allowNull(schema.getProperties().get("accept"));
				}
			}
		};
	}

	/**
	 * Adds null to the enum of the schema. The enum list springdoc hands over is unmodifiable, so it is replaced rather
	 * than appended to.
	 */
	@SuppressWarnings("unchecked")
	private static void allowNull(final Schema<?> schema) {
		final List<Object> values = new ArrayList<>();
		if (schema.getEnum() != null) {
			values.addAll(schema.getEnum());
		}
		if (!values.contains(null)) {
			values.add(null);
			((Schema<Object>) schema).setEnum(values);
		}
	}
}
