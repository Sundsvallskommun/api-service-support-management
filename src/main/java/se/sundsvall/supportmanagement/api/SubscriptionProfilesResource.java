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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportmanagement.api.model.subscription.SubscriptionProfile;
import se.sundsvall.supportmanagement.api.validation.groups.OnCreate;
import se.sundsvall.supportmanagement.api.validation.groups.OnUpdate;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.SubscriptionProfileService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/subscription-profiles")
@Tag(name = "Subscription profiles", description = "Subscription profile CRUD operations. A profile is a named set of event filters and the channels the events they match are delivered on.")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class SubscriptionProfilesResource {

	private final SubscriptionProfileService service;
	private final AccessControlService accessControlService;

	SubscriptionProfilesResource(final SubscriptionProfileService service, final AccessControlService accessControlService) {
		this.service = service;
		this.accessControlService = accessControlService;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List subscription profiles", description = "List the subscription profiles of the namespace.", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<SubscriptionProfile>> getSubscriptionProfiles(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId) {

		return ok(service.findSubscriptionProfiles(municipalityId, namespace));
	}

	@GetMapping(path = "/{profileId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get subscription profile", description = "Fetch a single subscription profile by id.", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<SubscriptionProfile> getSubscriptionProfile(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "profileId", description = "Subscription profile ID", example = "123e4567-e89b-12d3-a456-426614174000") @ValidUuid @PathVariable final String profileId) {

		return ok(service.findSubscriptionProfile(municipalityId, namespace, profileId));
	}

	@Validated(OnCreate.class)
	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create subscription profile", description = "Create a new subscription profile in the namespace.", responses = {
		@ApiResponse(responseCode = "201", description = "Created", headers = @Header(name = LOCATION, schema = @Schema(type = "string"))),
		@ApiResponse(responseCode = "409",
			description = "Conflict — a subscription profile with the same name already exists in the namespace",
			content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> createSubscriptionProfile(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Valid @NotNull @RequestBody final SubscriptionProfile subscriptionProfile) {

		accessControlService.verifyNamespaceAuthorization(namespace, municipalityId, ProtectedResource.SUBSCRIPTION_PROFILE, RW);

		final var id = service.createSubscriptionProfile(municipalityId, namespace, subscriptionProfile);
		return created(fromPath("/{municipalityId}/{namespace}/subscription-profiles/{id}")
			.buildAndExpand(municipalityId, namespace, id).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@Validated(OnUpdate.class)
	@PatchMapping(path = "/{profileId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Update subscription profile",
		description = "Partially update an existing subscription profile. Only non-null fields in the request body are applied; eventFilters and channels are replaced as a whole. " +
			"The change applies at once to every subscription pointing at the profile.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "409",
				description = "Conflict — a subscription profile with the same name already exists in the namespace",
				content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<SubscriptionProfile> updateSubscriptionProfile(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "profileId", description = "Subscription profile ID", example = "123e4567-e89b-12d3-a456-426614174000") @ValidUuid @PathVariable final String profileId,
		@Valid @NotNull @RequestBody final SubscriptionProfile subscriptionProfile) {

		accessControlService.verifyNamespaceAuthorization(namespace, municipalityId, ProtectedResource.SUBSCRIPTION_PROFILE, RW);

		return ok(service.updateSubscriptionProfile(municipalityId, namespace, profileId, subscriptionProfile));
	}

	@DeleteMapping(path = "/{profileId}", produces = ALL_VALUE)
	@Operation(summary = "Delete subscription profile", description = "Delete a subscription profile. All subscriptions pointing at the profile are also removed.", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation"),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "409",
			description = "Conflict — the profile is the reporter profile of the namespace",
			content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteSubscriptionProfile(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "profileId", description = "Subscription profile ID", example = "123e4567-e89b-12d3-a456-426614174000") @ValidUuid @PathVariable final String profileId) {

		accessControlService.verifyNamespaceAuthorization(namespace, municipalityId, ProtectedResource.SUBSCRIPTION_PROFILE, RW);

		service.deleteSubscriptionProfile(municipalityId, namespace, profileId);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}
}
