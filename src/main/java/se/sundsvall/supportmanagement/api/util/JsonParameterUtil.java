package se.sundsvall.supportmanagement.api.util;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.formatOrNull;

/**
 * What the resources accepting a JSON parameter share: the check a request needs before it reaches a service, and the
 * answer to a write.
 */
public final class JsonParameterUtil {

	private static final String KEY_MISMATCH = "Key in request body '%s' does not match key in path '%s'";

	private JsonParameterUtil() {}

	/**
	 * A key in the body that disagrees with the one in the path is a mistake worth naming rather than silently picking one
	 * of. A body that omits the key says nothing, and the path decides.
	 */
	public static void verifyKeyMatchesPath(final JsonParameter jsonParameter, final String key) {
		if ((jsonParameter.getKey() != null) && !key.equals(jsonParameter.getKey())) {
			throw Problem.valueOf(BAD_REQUEST, KEY_MISMATCH.formatted(jsonParameter.getKey(), key));
		}
	}

	/**
	 * The answer to a write that either created the parameter or replaced it: 201 with the location of the request when
	 * it is new, 200 when it was already there, and the version as ETag either way.
	 */
	public static ResponseEntity<JsonParameter> toUpsertResponse(final UpsertResult result) {
		final var response = result.created()
			? ResponseEntity.created(URI.create(ServletUriComponentsBuilder.fromCurrentRequest().build().getPath()))
			: ResponseEntity.ok();

		return response.header(ETAG, formatOrNull(result.jsonParameter().getVersion())).body(result.jsonParameter());
	}
}
