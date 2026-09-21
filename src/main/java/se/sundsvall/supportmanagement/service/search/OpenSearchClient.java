package se.sundsvall.supportmanagement.service.search;

import jakarta.persistence.EntityManagerFactory;
import org.elasticsearch.client.RestClient;
import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

/**
 * What is reached through Hibernate Search about the OpenSearch cluster and the errand index, for the few places that
 * go past the search DSL: the REST client, and the names the index is known by. Gathered here so that the unwrapping
 * of the backend is written once.
 */
@Component
public class OpenSearchClient {

	private final EntityManagerFactory entityManagerFactory;

	public OpenSearchClient(final EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	/** The low level client of the cluster. */
	public RestClient restClient() {
		return Search.mapping(entityManagerFactory).backend().unwrap(ElasticsearchBackend.class).client(RestClient.class);
	}

	/** The name writes to the errand index go to. */
	public String errandWriteIndex() {
		return Search.mapping(entityManagerFactory).indexedEntity(ErrandEntity.class).indexManager().unwrap(ElasticsearchIndexManager.class).descriptor().writeName();
	}

	/** The errand index as Hibernate Search knows it. */
	public IndexDescriptor errandIndex() {
		return Search.mapping(entityManagerFactory).indexedEntity(ErrandEntity.class).indexManager().descriptor();
	}
}
