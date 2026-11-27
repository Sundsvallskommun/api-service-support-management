package se.sundsvall.supportmanagement.service.search.index;

import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;
import org.hibernate.search.mapper.orm.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates the search index, or brings its mapping up to date, once the service is up.
 * <p>
 * Hibernate Search could do this while the service starts, but then the service could not start without OpenSearch,
 * and search is the one thing this service can do without. So the schema is put in place afterwards, off the thread
 * that started the service, with a few tries for an OpenSearch that is starting at the same time. Should it still not
 * be there, the nightly reindex or a full reindex creates it later, and the search endpoints report the failure until
 * then.
 * <p>
 * A cluster that refuses the mapping, answering 400, is refusing a change of a field it cannot make in place, which
 * only
 * a full reindex gets past. That is not tried again, and is reported by the health of the service, as is an index
 * of another layout than the service writes (see {@link SearchIndexHealth}). Neither keeps the service from starting:
 * this runs after it has.
 */
@Component
public class SearchSchemaInitializer {

	static final int ATTEMPTS = 6;
	static final Duration PAUSE_BETWEEN_ATTEMPTS = Duration.ofSeconds(10);
	static final String REFUSED = "Response: 400 ";

	// Unrolled, possessive and without an alternation that is repeated, so that a long reason does not recurse
	private static final Pattern REASON = Pattern.compile("\"reason\"\\s*+:\\s*+\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\"");

	private static final Logger LOG = LoggerFactory.getLogger(SearchSchemaInitializer.class);

	private final EntityManagerFactory entityManagerFactory;
	private final SearchAvailability availability;
	private final OpenSearchClient openSearch;
	private final SearchIndexHealth indexHealth;
	private final Duration pauseBetweenAttempts;

	@Autowired
	public SearchSchemaInitializer(final EntityManagerFactory entityManagerFactory, final SearchAvailability availability, final OpenSearchClient openSearch,
		final SearchIndexHealth indexHealth) {
		this(entityManagerFactory, availability, openSearch, indexHealth, PAUSE_BETWEEN_ATTEMPTS);
	}

	SearchSchemaInitializer(final EntityManagerFactory entityManagerFactory, final SearchAvailability availability, final OpenSearchClient openSearch,
		final SearchIndexHealth indexHealth, final Duration pauseBetweenAttempts) {
		this.entityManagerFactory = entityManagerFactory;
		this.availability = availability;
		this.openSearch = openSearch;
		this.indexHealth = indexHealth;
		this.pauseBetweenAttempts = pauseBetweenAttempts;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		if (availability.isEnabled()) {
			Thread.ofVirtual().name("search-schema").start(this::createOrUpdateSchema);
		}
	}

	/**
	 * Creates the index or brings its mapping up to date, as is done once the service is up.
	 */
	public void createOrUpdateSchema() {
		for (var attempt = 1; attempt <= ATTEMPTS; attempt++) {
			try {
				final var existed = openSearch.errandIndexExists();
				Search.mapping(entityManagerFactory).scope(Object.class).schemaManager().createOrUpdate();
				LOG.info("Search index schema is in place");

				// An index created here is empty and of the layout the service writes, an index that was there is to be looked at
				if (existed) {
					indexHealth.inspect();
				} else {
					indexHealth.created();
				}
				return;
			} catch (final Exception e) {
				// A cluster answering 400 is refusing the mapping, and asking again changes nothing. Anything else - a cluster
				// not there yet, or still recovering - is asked again
				final var refusal = refusalOf(e);
				if (refusal.isPresent()) {
					indexHealth.mappingNotUpdated(refusal.get());
					return;
				}
				if (attempt == ATTEMPTS) {
					LOG.error("Search index schema could not be created or updated after {} attempts. Search stays unavailable until the nightly reindex or a full reindex creates it", ATTEMPTS, e);
					return;
				}
				LOG.warn("Search index schema could not be created or updated (attempt {} of {}), trying again in {}: {}", attempt, ATTEMPTS, pauseBetweenAttempts, e.getMessage());
				pause();
			}
		}
	}

	/**
	 * Why the cluster refused the mapping, when it did: Hibernate Search says so with the answer of the cluster, a 400
	 * whose reason is the first one in its body.
	 */
	static Optional<String> refusalOf(final Throwable e) {
		for (var cause = e; cause != null; cause = cause.getCause()) {
			final var message = cause.getMessage();
			if (message != null && message.contains(REFUSED)) {
				final var reason = REASON.matcher(message);
				return Optional.of(reason.find() ? reason.group(1) : message.lines().findFirst().orElse(message));
			}
		}
		return Optional.empty();
	}

	private void pause() {
		try {
			Thread.sleep(pauseBetweenAttempts);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
