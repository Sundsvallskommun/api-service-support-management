package se.sundsvall.supportmanagement.service.search;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * Whether the environment has a search index at all. Hibernate Search is switched on per environment, and the search
 * endpoints answer 503 rather than fail obscurely where it is not.
 */
@Component
public class SearchAvailability {

	static final String SEARCH_DISABLED = "Search is not available in this environment";

	private final boolean enabled;

	public SearchAvailability(@Value("${spring.jpa.properties.hibernate.search.enabled:false}") final boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @throws org.springframework.web.ErrorResponseException 503 when the environment has no search index
	 */
	public void verifyEnabled() {
		if (!enabled) {
			throw Problem.valueOf(SERVICE_UNAVAILABLE, SEARCH_DISABLED);
		}
	}
}
