package se.sundsvall.supportmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportmanagement.api.model.subscription.Subscription;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;

import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/subscribers/{subscriberId}/subscriptions")
@Tag(name = "Subscriptions",
	description = "Subscription operations. A subscription describes what a subscriber listens for (an errand or all events in a namespace). Subscriptions support create, list and delete — to change what is being listened to, delete and create a new one.")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class SubscriptionsResource {

	private static final String NOT_IMPLEMENTED_MESSAGE = "Service layer pending — endpoint not yet implemented";

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List subscriptions", description = "List all subscriptions owned by the given subscriber.", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Subscriber not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<Subscription>> getSubscriptions(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "subscriberId", description = "Subscriber ID", example = "123e4567-e89b-12d3-a456-426614174000") @ValidUuid @PathVariable final String subscriberId) {

		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, NOT_IMPLEMENTED_MESSAGE);
	}

	@Validated(OnCreate.class)
	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create subscription",
		description = "Create a new subscription for the given subscriber. " +
			"The request body's target field decides what is being subscribed to: an errand (target.type=ERRAND, target.id=<errand UUID>) " +
			"or the whole namespace (target.type=NAMESPACE). " +
			"Optionally include eventFilters to override the subscriber's global filters for this subscription only.",
		responses = {
			@ApiResponse(responseCode = "201", description = "Created", headers = @Header(name = LOCATION, schema = @Schema(type = "string"))),
			@ApiResponse(responseCode = "404", description = "Subscriber not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "409", description = "Conflict — an equivalent subscription already exists", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> createSubscription(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "subscriberId", description = "Subscriber ID", example = "123e4567-e89b-12d3-a456-426614174000") @ValidUuid @PathVariable final String subscriberId,
		@Valid @NotNull @RequestBody final Subscription subscription) {

		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, NOT_IMPLEMENTED_MESSAGE);
	}

	@DeleteMapping(path = "/{subscriptionId}", produces = ALL_VALUE)
	@Operation(summary = "Delete subscription", description = "Delete a subscription owned by the given subscriber.", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation"),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteSubscription(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "subscriberId", description = "Subscriber ID", example = "123e4567-e89b-12d3-a456-426614174000") @ValidUuid @PathVariable final String subscriberId,
		@Parameter(name = "subscriptionId", description = "Subscription ID", example = "123e4567-e89b-12d3-a456-426614174000") @ValidUuid @PathVariable final String subscriptionId) {

		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, NOT_IMPLEMENTED_MESSAGE);
	}
}
