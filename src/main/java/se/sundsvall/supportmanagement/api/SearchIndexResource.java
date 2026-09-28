package se.sundsvall.supportmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.service.search.index.ErrandReindexService;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;

/**
 * The search index as a whole, which belongs to no namespace and to no municipality: one index holds the errands of all
 * of them.
 * <p>
 * Rebuilding a single namespace is an operation on that namespace and lives under it. Rebuilding the whole index is
 * not,
 * so it is not addressed as though it were, and it is guarded by every namespace that enforces access control rather
 * than by one named in a path.
 */
@RestController
@Validated
@RequestMapping("/search")
@Tag(name = "Errand search index", description = "The search index across every namespace")
class SearchIndexResource {

	private final ErrandReindexService reindexService;

	SearchIndexResource(final ErrandReindexService reindexService) {
		this.reindexService = reindexService;
	}

	@PostMapping(path = "/reindex", produces = ALL_VALUE)
	@Operation(summary = "Rebuild the whole search index",
		description = "Drops the whole search index and builds it again from the database, returning at once while the rebuild goes on in the background. " +
			"This is what a changed index mapping calls for. Every namespace of every municipality searches nothing until it is over, so it asks that the caller may " +
			"administer every namespace that enforces access control. Only one rebuild runs at a time, across this and the rebuild of a single namespace.",
		responses = {
			@ApiResponse(responseCode = "202", description = "Rebuild started", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "403", description = "A namespace that enforces access control is not the caller's to administer", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "409", description = "A rebuild is already running", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "503", description = "Search not available", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> reindexEverything() {
		reindexService.reindexEverything();
		return accepted().build();
	}
}
