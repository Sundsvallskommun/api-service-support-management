package se.sundsvall.supportmanagement.integration.elasticsearch;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

@CircuitBreaker(name = "jsonParameterElasticRepository")
public interface JsonParameterElasticRepository extends ElasticsearchRepository<JsonParameterDocument, String> {

	List<JsonParameterDocument> findByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	void deleteByErrandId(String errandId);
}
