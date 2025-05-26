package se.sundsvall.supportmanagement.api;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Set;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Conversation;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest;
import se.sundsvall.supportmanagement.api.model.communication.conversation.Message;
import se.sundsvall.supportmanagement.api.model.communication.conversation.MessageRequest;
import se.sundsvall.supportmanagement.service.CommunicationService;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/communication")
@Tag(name = "Errand communication", description = "Errand communication operations")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class ErrandCommunicationResource {

	private final CommunicationService service;
	private final ObjectMapper objectMapper;

	ErrandCommunicationResource(final CommunicationService service, final ObjectMapper objectMapper) {
		this.service = service;
		this.objectMapper = objectMapper;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(description = "Get all communications for an errand.", responses = {
		@ApiResponse(responseCode = "200", description = "OK - Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<Communication>> getCommunications(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId) {

		return ok(service.readCommunications(namespace, municipalityId, errandId));
	}

	@GetMapping(path = "/external", produces = APPLICATION_JSON_VALUE)
	@Operation(description = "Get all external communications for an errand.", responses = {
		@ApiResponse(responseCode = "200", description = "OK - Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<Communication>> getExternalCommunications(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId) {

		return ok(service.readExternalCommunications(namespace, municipalityId, errandId));
	}

	@PutMapping(path = "/{communicationId}/viewed/{isViewed}", produces = ALL_VALUE)
	@Operation(description = "Set viewed status for communication.", responses = {
		@ApiResponse(responseCode = "204", description = "No content - Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Void> updateViewedStatus(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Parameter(name = "communicationId", description = "communication ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("communicationId") final String communicationId,
		@Parameter(name = "isViewed", description = "If a message is viewed", example = "true") @PathVariable final boolean isViewed) {

		service.updateViewedStatus(namespace, municipalityId, errandId, communicationId, isViewed);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@PostMapping(path = "/email", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Send email to in context of an errand", description = "Sends an email message to the recipient specified in the request", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Void> sendEmail(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Valid @NotNull @RequestBody final EmailRequest request) {

		service.sendEmail(namespace, municipalityId, errandId, request);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@PostMapping(path = "/sms", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Send sms to in context of an errand", description = "Sends a sms message to the recipient specified in the request", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Void> sendSms(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Valid @NotNull @RequestBody final SmsRequest request) {

		service.sendSms(namespace, municipalityId, errandId, request);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@PostMapping(path = "/webmessage", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Send email to in context of an errand", description = "Sends an email message to the recipient specified in the request", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Void> sendWebMessage(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Valid @NotNull @RequestBody final WebMessageRequest request) {

		service.sendWebMessage(namespace, municipalityId, errandId, request);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(path = "/{communicationId}/attachments/{attachmentId}", produces = ALL_VALUE)
	@Operation(summary = "Get a streamed communication attachment.", description = "Fetches the communication attachment that matches the provided id in a streamed manner", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "507", description = "Insufficient Storage", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	void getMessageAttachment(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Parameter(name = "communicationId", description = "communication ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("communicationId") final String communicationId,
		@Parameter(name = "attachmentId", description = "Message attachment ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String attachmentId,
		final HttpServletResponse response) {

		service.getMessageAttachmentStreamed(namespace, municipalityId, errandId, communicationId, attachmentId, response);
	}

	@PostMapping(path = "/conversations", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create conversation", description = "Create new conversation", responses = {
		@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true),
	})
	ResponseEntity<Void> createConversation(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Valid @NotNull @RequestBody final ConversationRequest request) {

		final var conversationId = 0; // TODO: call service layer

		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/communication/conversations/{conversationId}")
			.buildAndExpand(municipalityId, namespace, errandId, conversationId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(path = "/conversations/{conversationId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get conversation", description = "Get existing conversation", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Conversation> getConversation(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Parameter(name = "conversationId", description = "Conversation ID", example = "1aefbbb8-de82-414b-b5d7-ba7c5bbe4506") @ValidUuid @PathVariable("conversationId") final String conversationId) {

		// TODO: call service layer

		return ok(Conversation.create());
	}

	@GetMapping(path = "/conversations", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get conversation", description = "Get existing conversation", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<Conversation>> getConversations(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId) {

		// TODO: call service layer
		return ok(List.of(Conversation.create()));
	}

	@PatchMapping(path = "/conversations/{conversationId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get conversation", description = "Get existing conversation", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Conversation> updateConversation(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Parameter(name = "conversationId", description = "Conversation ID", example = "1aefbbb8-de82-414b-b5d7-ba7c5bbe4506") @ValidUuid @PathVariable("conversationId") final String conversationId,
		@Valid @NotNull @RequestBody final ConversationRequest request) {

		// TODO: call service layer
		return ok(Conversation.create());
	}

	@PostMapping(path = "/conversations/{conversationId}/messages", consumes = MULTIPART_FORM_DATA_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create new message", description = "Create new message", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> createMessage(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Parameter(name = "conversationId", description = "Conversation ID", example = "1aefbbb8-de82-414b-b5d7-ba7c5bbe4506") @ValidUuid @PathVariable("conversationId") final String conversationId,
		@RequestPart("requestBody") @Schema(description = "Message body", implementation = MessageRequest.class) final String messageRequest,
		@RequestPart(value = "attachments", required = false) final List<MultipartFile> attachments) throws JsonProcessingException {

		final var messageRequestObj = objectMapper.readValue(messageRequest, MessageRequest.class);
		validate(messageRequestObj);

		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(path = "/conversations/{conversationId}/messages", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get message list", description = "Get message list", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<Message>> getMessages(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable("errandId") final String errandId,
		@Parameter(name = "conversationId", description = "Conversation ID", example = "1aefbbb8-de82-414b-b5d7-ba7c5bbe4506") @ValidUuid @PathVariable("conversationId") final String conversationId,
		@ParameterObject final Pageable pageable) {

		// TODO: call service layer
		return ok(List.of(Message.create()));
	}

	private <T> void validate(final T t) {
		final var validator = buildDefaultValidatorFactory().getValidator();
		final Set<ConstraintViolation<T>> violations = validator.validate(t);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}
}
