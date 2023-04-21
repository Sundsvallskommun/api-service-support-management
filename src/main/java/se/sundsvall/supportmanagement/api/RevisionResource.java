package se.sundsvall.supportmanagement.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

import java.util.List;

import org.hibernate.validator.constraints.Range;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.supportmanagement.api.model.revision.DifferenceResponse;
import se.sundsvall.supportmanagement.api.model.revision.Revision;

@RestController
@Validated
@Tag(name = "Revisions", description = "Errand and errand note revision operations")
public class RevisionResource {

	@GetMapping(path = "/errands/{id}/revisions", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Read errand revisions", description = "Returns all existing revisions for the errand that matches the provided id")
	@ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Revision.class))))
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public ResponseEntity<List<Revision>> getErrandRevisions(
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String id) {

		// TODO Implement
		return ok(List.of(Revision.create()));
	}

	@GetMapping(path = "/errands/{id}/revisions/{revisionId}", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Read a specific revision of the errand", description = "Returns the content for the requested revision of an errand")
	@ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = String.class)))
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public ResponseEntity<String> getErrandRevision(
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String id,
		@Parameter(name = "revisionId", description = "Revision id", example = "edfc2b33-fb8e-4fc4-87b0-3796bee15073") @ValidUuid @PathVariable final String revisionId) {

		// TODO Implement
		return ok("{}");
	}

	@GetMapping(path = "/errands/{id}/revisions/difference", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Compare differences between two revision versions of an errand", description = "Returns the differences between the source and target revision for the provided errand id")
	@ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = DifferenceResponse.class)))
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public ResponseEntity<DifferenceResponse> getErrandDiffByVersions(
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String id,
		@Parameter(name = "source", description = "Source version to compare", example = "0", required = true) @Range(min = 0, max = Integer.MAX_VALUE) @RequestParam final Integer source,
		@Parameter(name = "target", description = "Target version to compare", example = "1", required = true) @Range(min = 0, max = Integer.MAX_VALUE) @RequestParam final Integer target) {

		// TODO Implement
		return ok(DifferenceResponse.create());
	}

	@GetMapping(path = "/errands/{id}/notes/{noteId}/revisions", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Read note revisions", description = "Returns all existing revisions for the errand note that matches the provided errand id and note id")
	@ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Revision.class))))
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public ResponseEntity<List<Revision>> getErrandNoteRevisions(
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String id,
		@Parameter(name = "noteId", description = "Errand note id", example = "22a57c09-47ca-45bf-ab02-819011c6fd8d") @ValidUuid @PathVariable final String noteId) {

		// TODO Implement
		return ok(List.of(Revision.create()));
	}

	@GetMapping(path = "/errands/{id}/notes/{noteId}/revisions/{revisionId}", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Read a specific revision of an errand note", description = "Returns the content for the requested revision of an errand note")
	@ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = String.class)))
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public ResponseEntity<String> getErrandNoteRevision(
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String id,
		@Parameter(name = "noteId", description = "Errand note id", example = "22a57c09-47ca-45bf-ab02-819011c6fd8d") @ValidUuid @PathVariable final String noteId,
		@Parameter(name = "revisionId", description = "Revision id", example = "edfc2b33-fb8e-4fc4-87b0-3796bee15073") @ValidUuid @PathVariable final String revisionId) {

		// TODO Implement
		return ok("{}");
	}

	@GetMapping(path = "/errands/{id}/notes/{noteId}/revisions/difference", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	@Operation(summary = "Compare differences between two revision versions of an errand note", description = "Returns the differences between the source and target revision for the provided errand id and note id")
	@ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = DifferenceResponse.class)))
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
	@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	public ResponseEntity<DifferenceResponse> getErrandNoteDiffByVersions(
		@Parameter(name = "id", description = "Errand id", example = "b82bd8ac-1507-4d9a-958d-369261eecc15") @ValidUuid @PathVariable final String id,
		@Parameter(name = "noteId", description = "Errand note id", example = "22a57c09-47ca-45bf-ab02-819011c6fd8d") @ValidUuid @PathVariable final String noteId,
		@Parameter(name = "source", description = "Source version to compare", example = "0", required = true) @Range(min = 0, max = Integer.MAX_VALUE) @RequestParam final Integer source,
		@Parameter(name = "target", description = "Target version to compare", example = "1", required = true) @Range(min = 0, max = Integer.MAX_VALUE) @RequestParam final Integer target) {

		// TODO Implement
		return ok(DifferenceResponse.create());
	}
}
