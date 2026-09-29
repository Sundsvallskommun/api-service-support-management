package se.sundsvall.supportmanagement.service.search.index;

import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
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
 * be there, a full reindex creates it later, and the search endpoints report the failure until then.
 */
@Component
public class SearchSchemaInitializer {

	static final int ATTEMPTS = 6;
	static final Duration PAUSE_BETWEEN_ATTEMPTS = Duration.ofSeconds(10);

	private static final Logger LOG = LoggerFactory.getLogger(SearchSchemaInitializer.class);

	private final EntityManagerFactory entityManagerFactory;
	private final SearchAvailability availability;
	private final Duration pauseBetweenAttempts;

	@Autowired
	public SearchSchemaInitializer(final EntityManagerFactory entityManagerFactory, final SearchAvailability availability) {
		this(entityManagerFactory, availability, PAUSE_BETWEEN_ATTEMPTS);
	}

	SearchSchemaInitializer(final EntityManagerFactory entityManagerFactory, final SearchAvailability availability, final Duration pauseBetweenAttempts) {
		this.entityManagerFactory = entityManagerFactory;
		this.availability = availability;
		this.pauseBetweenAttempts = pauseBetweenAttempts;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		if (availability.isEnabled()) {
			Thread.ofVirtual().name("search-schema").start(this::createOrUpdateSchema);
		}
	}

	void createOrUpdateSchema() {
		for (var attempt = 1; attempt <= ATTEMPTS; attempt++) {
			try {
				Search.mapping(entityManagerFactory).scope(Object.class).schemaManager().createOrUpdate();
				LOG.info("Search index schema is in place");
				return;
			} catch (final RuntimeException e) {
				if (attempt == ATTEMPTS) {
					LOG.error("Search index schema could not be created or updated after {} attempts. Search stays unavailable until a full reindex creates it", ATTEMPTS, e);
					return;
				}
				LOG.warn("Search index schema could not be created or updated (attempt {} of {}), trying again in {}: {}", attempt, ATTEMPTS, pauseBetweenAttempts, e.getMessage());
				pause();
			}
		}
	}

	private void pause() {
		try {
			Thread.sleep(pauseBetweenAttempts);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
