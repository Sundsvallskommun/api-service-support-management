package se.sundsvall.supportmanagement.service.search;

import jakarta.persistence.EntityManager;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Component;

/**
 * The one thing code outside the search may ask of the indexing that follows every commit: to leave some of it out.
 * <p>
 * Keeps Hibernate Search out of the services that write the database, which neither know nor should know that an
 * index follows them.
 */
@Component
public class SearchIndexing {

	private final EntityManager entityManager;
	private final SearchAvailability availability;

	public SearchIndexing(final EntityManager entityManager, final SearchAvailability availability) {
		this.entityManager = entityManager;
		this.availability = availability;
	}

	/**
	 * Leaves changes to sent in types out of the indexing for the rest of the current transaction. For a type that is
	 * held inside the errand's document, this also leaves the errand from being rebuilt on their account.
	 *
	 * @param types the entity types whose changes are not to be indexed
	 */
	public void withoutIndexingOf(final Class<?>... types) {
		if (!availability.isEnabled()) {
			return;
		}
		Search.session(entityManager).indexingPlanFilter(context -> {
			for (final var type : types) {
				context.exclude(type);
			}
		});
	}
}
