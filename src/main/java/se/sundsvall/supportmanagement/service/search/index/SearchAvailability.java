package se.sundsvall.supportmanagement.service.search.index;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * Whether this instance can search at all. Hibernate Search is switched on per environment, and the search endpoints
 * answer 503 rather than fail obscurely where it is not.
 * <p>
 * Search may also be given up on after startup, by {@link ErrandIndexModel} finding an index that does not hold what
 * the service binds to it. Searching an index the bindings misread is what access control is rendered from, so it is
 * not attempted; everything else the service does is unaffected, which is why this switches search off rather than
 * keeping the service from starting.
 */
@Component
public class SearchAvailability {

	static final String SEARCH_DISABLED = "Search is not available in this environment";
	static final String SEARCH_GIVEN_UP_ON = "Search is not available: %s";

	private final boolean enabled;

	private volatile String unusable;

	public SearchAvailability(@Value("${spring.jpa.properties.hibernate.search.enabled:false}") final boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/** Why search is not to be attempted on this instance, empty while it is. */
	public Optional<String> unusable() {
		return Optional.ofNullable(unusable);
	}

	/**
	 * Gives up on search for the life of this instance, sent in reason being what the endpoints answer with.
	 */
	public void giveUp(final String reason) {
		this.unusable = reason;
	}

	/**
	 * @throws org.springframework.web.ErrorResponseException 503 when the environment has no search index
	 */
	public void verifyEnabled() {
		if (!enabled) {
			throw Problem.valueOf(SERVICE_UNAVAILABLE, SEARCH_DISABLED);
		}
		if (unusable != null) {
			throw Problem.valueOf(SERVICE_UNAVAILABLE, SEARCH_GIVEN_UP_ON.formatted(unusable));
		}
	}
}
