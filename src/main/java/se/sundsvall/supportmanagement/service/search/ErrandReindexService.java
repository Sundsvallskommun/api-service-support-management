package se.sundsvall.supportmanagement.service.search;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.config.SearchProperties;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Rebuilds the search index from the database.
 * <p>
 * The index is disposable: the database is the source of truth, and a document that was not written because OpenSearch
 * was unreachable at the time, or that predates a change to the mapping, is put right by a rebuild. One rebuild runs at
 * a time across every pod of the service, guarded by the same lock table the schedulers use, since two mass indexers
 * on the same index only slow each other down.
 */
@Service
public class ErrandReindexService {

	static final String LOCK_NAME = "errand-reindex";
	static final String REINDEX_RUNNING = "A reindex is already running";

	private static final Logger LOG = LoggerFactory.getLogger(ErrandReindexService.class);
	private static final String NAMESPACE_CONDITION = "e.namespace = :namespace and e.municipalityId = :municipalityId";
	private static final String PURGE_QUERY = """
		{"query": {"bool": {"filter": [{"term": {"municipalityId": "%s"}}, {"term": {"namespace": "%s"}}]}}}""";

	private final EntityManagerFactory entityManagerFactory;
	private final LockProvider lockProvider;
	private final SearchAvailability availability;
	private final SearchProperties properties;
	private final AccessControlService accessControlService;

	public ErrandReindexService(final EntityManagerFactory entityManagerFactory, final LockProvider lockProvider, final SearchAvailability availability, final SearchProperties properties,
		final AccessControlService accessControlService) {
		this.entityManagerFactory = entityManagerFactory;
		this.lockProvider = lockProvider;
		this.availability = availability;
		this.properties = properties;
		this.accessControlService = accessControlService;
	}

	/**
	 * Starts a rebuild and returns at once, the rebuild goes on in the background and reports in the log.
	 * <p>
	 * A rebuild of a namespace first removes what the index holds for the namespace, so that an errand deleted while the
	 * index was out of reach does not linger, and then writes its errands anew, leaving the rest of the index as it is. A
	 * full rebuild drops the index and creates it anew from the mapping, which is what a changed mapping calls for, and
	 * empties the search of every namespace until it is done. Either way the namespace is searched with a hole in it
	 * until the rebuild is over, and an errand changed while the rebuild is passing over it may be written back as it was
	 * before the change, which its next change puts right.
	 * <p>
	 * Held to the same grant as the configuration of the namespace, since a rebuild is an operation on the namespace
	 * rather than on any errand, and a full rebuild reaches every namespace there is.
	 *
	 * @param  namespace                                      namespace to rebuild
	 * @param  municipalityId                                 municipality id
	 * @param  full                                           true to rebuild the whole index across every namespace,
	 *                                                        recreating the schema
	 * @throws org.springframework.web.ErrorResponseException 403 when the user may not administer the namespace, 409
	 *                                                        when a rebuild is already running, 503 when the environment
	 *                                                        has no search index
	 */
	public void reindex(final String namespace, final String municipalityId, final boolean full) {
		availability.verifyEnabled();
		accessControlService.verifyNamespaceAuthorization(namespace, municipalityId, ProtectedResource.NAMESPACE_CONFIG, RW);

		final var lock = lockProvider.lock(new LockConfiguration(Instant.now(), LOCK_NAME, properties.reindex().lockAtMostFor(), Duration.ZERO))
			.orElseThrow(() -> Problem.valueOf(CONFLICT, REINDEX_RUNNING));

		final var description = full ? "the whole errand index" : "errands of namespace '%s' for municipality '%s'".formatted(namespace, municipalityId);
		LOG.info("Reindex of {} starting", description);

		try {
			if (!full) {
				purgeNamespace(namespace, municipalityId);
			}
			massIndexer(namespace, municipalityId, full)
				.start()
				.whenComplete((_, throwable) -> finish(lock, description, throwable));
		} catch (final RuntimeException e) {
			lock.unlock();
			throw e;
		}
	}

	private MassIndexer massIndexer(final String namespace, final String municipalityId, final boolean full) {
		final var massIndexer = Search.mapping(entityManagerFactory).scope(ErrandEntity.class).massIndexer();

		if (full) {
			return massIndexer.dropAndCreateSchemaOnStart(true);
		}

		massIndexer.type(ErrandEntity.class).reindexOnly(NAMESPACE_CONDITION)
			.param("namespace", namespace)
			.param("municipalityId", municipalityId);

		// The purge that is on by default would empty the index of every namespace, not only the one being rebuilt
		return massIndexer.purgeAllOnStart(false);
	}

	/**
	 * Removes the documents of the namespace. Hibernate Search purges an index whole or by tenant and knows no tenants
	 * here, so this goes to OpenSearch directly with the same two fields the search filters on.
	 */
	private void purgeNamespace(final String namespace, final String municipalityId) {
		final var mapping = Search.mapping(entityManagerFactory);
		final var index = mapping.indexedEntity(ErrandEntity.class).indexManager().unwrap(ElasticsearchIndexManager.class).descriptor().writeName();
		final var client = mapping.backend().unwrap(ElasticsearchBackend.class).client(RestClient.class);

		final var request = new Request("POST", "/" + index + "/_delete_by_query");
		request.addParameter("conflicts", "proceed");
		request.addParameter("refresh", "true");
		request.setJsonEntity(PURGE_QUERY.formatted(municipalityId, namespace));

		try {
			client.performRequest(request);
		} catch (final IOException e) {
			throw new UncheckedIOException("Documents of namespace '%s' for municipality '%s' could not be removed from the search index".formatted(namespace, municipalityId), e);
		}
	}

	private static void finish(final SimpleLock lock, final String description, final Throwable throwable) {
		try {
			if (throwable == null) {
				LOG.info("Reindex of {} finished", description);
			} else {
				LOG.error("Reindex of {} failed", description, throwable);
			}
		} finally {
			lock.unlock();
		}
	}
}
