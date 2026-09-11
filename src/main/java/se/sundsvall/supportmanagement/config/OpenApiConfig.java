package se.sundsvall.supportmanagement.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import static se.sundsvall.supportmanagement.service.util.ServiceUtil.REQUEST_GROUP_ID_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.TRIGGER_PROCESS_HEADER;

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
	OperationCustomizer triggerProcessHeaderCustomizer() {
		return (Operation operation, @SuppressWarnings("unused") HandlerMethod handlerMethod) -> {
			operation.addParametersItem(new Parameter()
				.name(TRIGGER_PROCESS_HEADER)
				.in("header")
				.required(false)
				.description("""
					Set to 'false' by a process engine writing to an errand it is itself running, to keep the write from \
					waking that process again. An omitted header, and any other value, wakes it. Disregarded for ad \
					account identities, whose writes always wake the process.""")
				.example("false")
				.schema(new StringSchema()));
			return operation;
		};
	}
}
