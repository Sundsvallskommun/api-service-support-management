package se.sundsvall.supportmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.SubscriptionProfileMemberService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/subscription-profiles/{profileId}/members")
@Tag(name = "Subscription profiles", description = "Subscription profile CRUD operations. A profile is a named set of event filters and the channels the events they match are delivered on.")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class SubscriptionProfileMembersResource {

	private final SubscriptionProfileMemberService service;
	private final AccessControlService accessControlService;

	SubscriptionProfileMembersResource(final SubscriptionProfileMemberService service, final AccessControlService accessControlService) {
		this.service = service;
		this.accessControlService = accessControlService;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List subscription profile members", description = "List the principals holding a namespace subscription to the profile.", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<Identifier>> getSubscriptionProfileMembers(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "profileId", description = "Subscription profile ID", example = "123e4567-e89b-12d3-a456-426614174000") @ValidUuid @PathVariable final String profileId) {

		return ok(service.findMembers(municipalityId, namespace, profileId));
	}

	@PutMapping(consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Replace subscription profile members",
		description = "Makes the given principals the members of the profile. Principals not yet members are subscribed to the profile for the whole namespace, " +
			"creating a subscriber for them when they have none, and members missing from the list lose their subscription to the profile. " +
			"A principal who has left the profile themselves is not subscribed again.",
		responses = {
			@ApiResponse(responseCode = "204", description = "Successful operation"),
			@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> replaceSubscriptionProfileMembers(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "profileId", description = "Subscription profile ID", example = "123e4567-e89b-12d3-a456-426614174000") @ValidUuid @PathVariable final String profileId,
		@NotNull @RequestBody final List<@Valid @NotNull Identifier> members) {

		accessControlService.verifyNamespaceAuthorization(namespace, municipalityId, ProtectedResource.SUBSCRIPTION_PROFILE, RW);

		service.syncMembers(municipalityId, namespace, profileId, members);
		return noContent()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}
}
