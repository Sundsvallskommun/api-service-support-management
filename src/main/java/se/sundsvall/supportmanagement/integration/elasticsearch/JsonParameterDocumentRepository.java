package se.sundsvall.supportmanagement.integration.elasticsearch;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import se.sundsvall.supportmanagement.integration.elasticsearch.model.JsonParameterDocument;

@CircuitBreaker(name = "jsonParameterDocumentRepository")
public interface JsonParameterDocumentRepository extends ElasticsearchRepository<JsonParameterDocument, String> {
}
