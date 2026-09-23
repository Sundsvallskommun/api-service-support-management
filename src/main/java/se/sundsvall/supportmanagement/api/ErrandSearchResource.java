package se.sundsvall.supportmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.search.ErrandIndex;
import se.sundsvall.supportmanagement.service.search.ErrandSearchService;
import se.sundsvall.supportmanagement.service.search.index.ErrandReindexService;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.supportmanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/search")
@Tag(name = "Errand search", description = "Full text search of errands")
class ErrandSearchResource {

	private static final String ERRAND_FIELDS = "`" + ErrandIndex.ERRAND_NUMBER + "`, `" + ErrandIndex.TITLE + "`, `" + ErrandIndex.DESCRIPTION + "`, `" + ErrandIndex.CONTACT_REASON_DESCRIPTION
		+ "`, `" + ErrandIndex.CONTACT_REASON + ".reason`, `" + ErrandIndex.STATUS + "`, `" + ErrandIndex.CATEGORY + "`, `" + ErrandIndex.TYPE + "`, `" + ErrandIndex.RESOLUTION + "`, `"
		+ ErrandIndex.CHANNEL + "`, `" + ErrandIndex.PRIORITY + "`, `" + ErrandIndex.REPORTER_USER_ID + "`, `" + ErrandIndex.ASSIGNED_USER_ID + "`, `" + ErrandIndex.ASSIGNED_GROUP_ID + "`, `"
		+ ErrandIndex.ESCALATION_EMAIL + "`, `" + ErrandIndex.BUSINESS_RELATED + "`, `" + ErrandIndex.CREATED + "`, `" + ErrandIndex.MODIFIED + "`, `" + ErrandIndex.TOUCHED + "`, `"
		+ ErrandIndex.SUSPENDED_FROM + "`, `" + ErrandIndex.SUSPENDED_TO + "`, `" + ErrandIndex.EXTERNAL_TAGS + ".key`, `" + ErrandIndex.EXTERNAL_TAG_VALUE + "`, `" + ErrandIndex.LABELS + "."
		+ ErrandIndex.METADATA_LABEL_ID + "`, `" + ErrandIndex.ATTACHMENTS + ".fileName`, `" + ErrandIndex.ATTACHMENTS + ".mimeType`.";

	static final String QUERY_DESCRIPTION = """
		A [Lucene query string](https://opensearch.org/docs/latest/query-dsl/full-text/query-string/), searched in an index \
		of the errands and everything attached to them. Blank matches every errand. Words are required unless the query \
		says otherwise, and matching ignores case; text fields are also stemmed for Swedish.

		| Syntax | Meaning |
		|---|---|
		| `vatten läcka` | both words, in any of the text fields |
		| `vatten OR läcka`, `vatten AND NOT läcka`, `-läcka` | boolean operators, upper case |
		| `"vatten läcka"`, `"vatten läcka"~2` | exact phrase, phrase allowing 2 words in between |
		| `status:new`, `stakeholders.lastName:berg` | a value in a specific field |
		| `title:(vatten OR gas)` | several terms against one field |
		| `jsonParameters.vehicle.regNo:abc123`, `jsonParameters.vehicle.regNo.raw:abc123` | a JSON parameter by key and path, as words or as the exact value |
		| `berg*`, `b?rg`, `*ander*` | wildcards in a value |
		| `jsonParameters.\\*.regNo:abc123`, `\\*.probability:3` | a wildcard in a field name, escaped, standing for any part of the path |
		| `bergh~1` | fuzzy, at most one edit away |
		| `created:[2025-01-01 TO 2025-12-31]`, `created:>=2025-06-01`, `created:{* TO now-7d}` | ranges, with date math |
		| `_exists_:assignedUserId` | the field has a value |
		| `title:vatten^3 description:vatten` | boost a clause |

		A bare word is a search term, it is only a field name when followed by a colon. The characters \
		`+ - = && || > < ! ( ) { } [ ] ^ " ~ * ? : \\ /` are part of the syntax and are escaped with a backslash when meant literally.

		Fields of the errand: \
		""" + ERRAND_FIELDS + """

		Of its stakeholders: `stakeholders.externalId`, `stakeholders.externalIdType`, `stakeholders.role`, `stakeholders.firstName`, \
		`stakeholders.lastName`, `stakeholders.organizationName`, `stakeholders.address`, `stakeholders.careOf`, `stakeholders.zipCode`, \
		`stakeholders.city`, `stakeholders.country`, `stakeholders.contactChannels.type`, `stakeholders.contactChannels.value`, \
		`stakeholders.parameters.key`, `stakeholders.parameters.values`.
		Of its parameters: `parameters.key`, `parameters.displayName`, `parameters.parameterGroup`, `parameters.values`, \
		`jsonParameters.<key>.<path>` for every scalar in a JSON parameter (`.raw` appended for the exact value).
		Of its phases: `phases.phase.name`, `phases.phase.displayName`, `phases.started`, `phases.ended`.
		Of its measures, decisions, statements and investigations, under `measures.`, `decisions.`, `statements.` and \
		`investigations.`: `type`, `status`, `title`, `description`, `dueAt`, `completedAt`, `createdBy`, `modifiedBy`, `created`, \
		their own properties by the names of the API model, and `jsonParameters.<key>.<path>`.
		Of its communications: `communications.subject`, `communications.messageBody`, `communications.sender`, \
		`communications.direction`, `communications.type`, `communications.sent`.

		A search without a field looks in the text of all of the above.

		Where a namespace enforces access control, what the user may not read they may not search either: the fields of a \
		resource their labels do not reach (communications, decisions, statements, investigations, measures, parameters, JSON \
		parameters, attachments), the fields their roles keep from them, and the keys of parameters and JSON parameters their \
		roles do not grant. Such fields are left out of a search without a field, and a query naming one of them, or sorting on \
		one, is refused with 403, as is a wildcard in a field name.

		An errand is searched by what the user may read of it, which differs with how they hold it: an errand their labels cover \
		is searched by everything their roles allow, one they cover at limited read only by what the namespace exposes for a \
		limited read, and one they reported by its reporter fields. A query naming a field of one of these and not of another is \
		answered from the errands where it may be read, without a refusal; it is refused only when no errand of the user can \
		answer it.""";

	static final String SORT_DESCRIPTION = "Without a sort the best matches come first, newest first among equals. Sortable properties: " +
		"created, modified, touched, suspendedFrom, suspendedTo, errandNumber, title, status, category, type, priority, resolution, channel, " +
		"reporterUserId, assignedUserId, assignedGroupId.";

	private final ErrandSearchService searchService;
	private final ErrandReindexService reindexService;

	ErrandSearchResource(final ErrandSearchService searchService, final ErrandReindexService reindexService) {
		this.searchService = searchService;
		this.reindexService = reindexService;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Search errands", description = "Searches the errands the requesting user has access to, ranked by how well they match. " + SORT_DESCRIPTION, responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		}))),
		@ApiResponse(responseCode = "403", description = "The query names a resource the user may not read", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "503", description = "Search not available", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Page<Errand>> searchErrands(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "query", description = QUERY_DESCRIPTION, example = "vattenläcka status:new stakeholders.lastName:berg") @RequestParam(required = false) final String query,
		@ParameterObject final Pageable pageable) {

		return ok(searchService.search(namespace, municipalityId, query, pageable));
	}

	@PostMapping(path = "/reindex", produces = ALL_VALUE)
	@Operation(summary = "Rebuild the search index",
		description = "Rebuilds the search index of the namespace from the database and returns at once, the rebuild goes on in the background. " +
			"Needed after an outage of the search cluster, and for errands created before search was introduced. With full=true the whole index, across every namespace, is dropped and " +
			"rebuilt from scratch, which is what a changed index mapping calls for. Only one rebuild runs at a time.",
		responses = {
			@ApiResponse(responseCode = "202", description = "Rebuild started", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
				Problem.class, ConstraintViolationProblem.class
			}))),
			@ApiResponse(responseCode = "403", description = "The user may not administer the namespace", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "409", description = "A rebuild is already running", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "503", description = "Search not available", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> reindexErrands(
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "full", description = "Rebuild the whole index across every namespace, recreating its schema") @RequestParam(defaultValue = "false") final boolean full) {

		reindexService.reindex(namespace, municipalityId, full);
		return accepted().build();
	}
}
