package se.sundsvall.supportmanagement.api;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATON_MESSAGE;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.supportmanagement.api.model.metadata.ExternalIdType;
import se.sundsvall.supportmanagement.service.MetadataService;

@RestController
@Validated
@RequestMapping("/{namespace}/{municipalityId}/metadata/externalIdTypes")
@Tag(name = "Metadata for externalid types", description = "Externaid type metadata operations")
public class MetadataExternalIdTypeResource {

	@Autowired
	private MetadataService metadataService;

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = { APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Create externalid type", description = "Create new externalid type for the namespace and municipality")
	@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", content = @Content(mediaType = ALL_VALUE, schema = @Schema(implementation = Void.class)))
	@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	ResponseEntity<Void> createExternalIdType(final UriComponentsBuilder uriComponentsBuilder,
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Valid @NotNull @RequestBody final ExternalIdType body) {
		return created(uriComponentsBuilder.path("/{namespace}/{municipalityId}/metadata/externalIdTypes/{externalIdType}")
			.buildAndExpand(namespace, municipalityId, metadataService.createExternalIdType(namespace, municipalityId, body)).toUri()).header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@GetMapping(path = "/{externalIdType}", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Get externalid type", description = "Get externalid type matching sent in namespace, municipality and externalid type")
	@ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = ExternalIdType.class)))
	@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	ResponseEntity<ExternalIdType> getExternalIdType(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "externalIdType", description = "Name of externalid type", example = "PRIVATE") @PathVariable final String externalIdType) {
		return ok(metadataService.getExternalIdType(namespace, municipalityId, externalIdType));
	}

	@GetMapping(produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Get externalid types", description = "Get all externalid types for the namespace and municipality")
	@ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ExternalIdType.class))))
	@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	ResponseEntity<List<ExternalIdType>> getExternalIdTypes(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId) {
		return ok(metadataService.findExternalIdTypes(namespace, municipalityId));
	}

	@DeleteMapping(path = "/{externalIdType}", produces = { APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Delete externalid type", description = "Delete externalid type matching namespace, municipality and externalIdType")
	@ApiResponse(responseCode = "204", description = "Successful operation")
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	ResponseEntity<Void> deleteExternalIdType(
		@Parameter(name = "namespace", description = "Namespace", example = "my.namespace") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATON_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "externalIdType", description = "Name of externalid type", example = "PRIVATE") @PathVariable final String externalIdType) {
		metadataService.deleteExternalIdType(namespace, municipalityId, externalIdType);
		return noContent().build();
	}
}
