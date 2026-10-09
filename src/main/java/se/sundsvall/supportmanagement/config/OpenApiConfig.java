package se.sundsvall.supportmanagement.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import static se.sundsvall.supportmanagement.service.util.ServiceUtil.NOTIFY_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.REQUEST_GROUP_ID_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.TRIGGER_PROCESS_HEADER;

@Configuration
class OpenApiConfig {

	private static final String IN_HEADER = "header";

	@Bean
	OperationCustomizer headerCustomizer() {
		return (Operation operation, @SuppressWarnings("unused") HandlerMethod handlerMethod) -> operation
			.addParametersItem(new Parameter()
				.name(REQUEST_GROUP_ID_HEADER)
				.in(IN_HEADER)
				.required(false)
				.description("Optional UUID that groups related events and notifications for this operation. If omitted, no grouping is applied.")
				.example("f47ac10b-58cc-4372-a567-0e02b2c3d479")
				.schema(new StringSchema().format("uuid")))
			.addParametersItem(new Parameter()
				.name(TRIGGER_PROCESS_HEADER)
				.in(IN_HEADER)
				.required(false)
				.description("""
					Set to 'false' by a process engine writing to an errand it is itself running, to keep the write from \
					waking that process again. An omitted header, and any other value, wakes it. Disregarded for ad \
					account identities, whose writes always wake the process.""")
				.example("false")
				.schema(new StringSchema()));
	}

	/**
	 * The header is read by a servlet filter, which springdoc does not see, so it is declared here.
	 */
	@Bean
	OperationCustomizer notifyHeaderCustomizer() {
		return (Operation operation, @SuppressWarnings("unused") HandlerMethod handlerMethod) -> {
			operation.addParametersItem(new Parameter()
				.name(NOTIFY_HEADER)
				.in(IN_HEADER)
				.required(false)
				.description(
					"Set to false to keep the operation from notifying the subscribers of the errand, on any channel. A change to the errand itself then notifies no one directly either, while a change to a note still notifies the users it would notify directly. Any other value, or leaving the header out, notifies as usual.")
				.example(false)
				.schema(new BooleanSchema()._default(true)));
			return operation;
		};
	}
}
