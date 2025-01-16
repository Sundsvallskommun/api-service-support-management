package se.sundsvall.supportmanagement.api;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static org.zalando.problem.Status.CONFLICT;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.service.NotificationService;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}")
@Tag(name = "User notifications", description = "User notifications operations")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class NotificationsResource {

	private final NotificationService notificationService;

	NotificationsResource(final NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping(path = "/errands/{errandId}/notifications/{notificationId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get notifications", description = "Get a specific notification for the namespace and municipality", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Notification> getNotification(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "notificationId", description = "notification ID", example = "74540a24-70e1-4e82-90f7-7d8ad4666cdc") @ValidUuid @PathVariable final String notificationId) {

		return ok(notificationService.getNotification(municipalityId, namespace, errandId, notificationId));
	}

	@GetMapping(path = "/errands/{errandId}/notifications", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get notifications", description = "Get all notifications for the namespace, municipality and errand", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<Notification>> getNotificationsByErrandId(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@ParameterObject final Sort sort) {

		return ok(notificationService.getNotificationsByErrandId(municipalityId, namespace, errandId, sort));
	}

	@PostMapping(path = "/errands/{errandId}/notifications", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create notification", description = "Create new notification for the namespace and municipality", responses = {
		@ApiResponse(responseCode = "201", description = "Created - Successful operation", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), useReturnTypeSchema = true),
		@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> createNotification(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final Notification notification) {

		final var result = Optional.ofNullable(notificationService.createNotification(municipalityId, namespace, errandId, notification))
			.orElseThrow(() -> Problem.valueOf(CONFLICT, "Notification already exists"));

		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}//notifications/{notificationId}")
			.buildAndExpand(municipalityId, namespace, errandId, result).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@DeleteMapping(path = "/errands/{errandId}/notifications/{notificationId}", produces = ALL_VALUE)
	@Operation(summary = "Delete notification", description = "Delete notification for the namespace and municipality", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteNotification(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "errandId", description = "Errand ID", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "notificationId", description = "Notification ID", example = "74540a24-70e1-4e82-90f7-7d8ad4666cdc") @ValidUuid @PathVariable final String notificationId) {

		notificationService.deleteNotification(municipalityId, namespace, errandId, notificationId);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	/*
	 * Global notifications path (not coupled with a specific errand)
	 */

	@GetMapping(path = "/notifications", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get notifications", description = "Get notifications for the namespace and municipality with the specified ownerId", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<Notification>> getNotificationsByOwnerId(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "ownerId", description = "owner ID", example = "12") @RequestParam final String ownerId) {

		return ok(notificationService.getNotificationsByOwnerId(municipalityId, namespace, ownerId));
	}

	@PatchMapping(path = "/notifications", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Update notification", description = "Update notifications for the namespace and municipality", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> updateNotifications(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Valid @NotEmpty @RequestBody final List<Notification> notifications) {

		notificationService.updateNotifications(municipalityId, namespace, notifications);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}
}
