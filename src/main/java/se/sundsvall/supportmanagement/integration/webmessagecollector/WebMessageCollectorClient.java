package se.sundsvall.supportmanagement.integration.webmessagecollector;

import static se.sundsvall.supportmanagement.integration.webmessagecollector.configuration.WebMessageCollectorConfiguration.CLIENT_ID;

import generated.se.sundsvall.webmessagecollector.MessageDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import se.sundsvall.supportmanagement.integration.webmessagecollector.configuration.WebMessageCollectorConfiguration;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.web-message-collector.url}",
	configuration = WebMessageCollectorConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface WebMessageCollectorClient {

	@GetMapping("/{municipalityId}/messages/{familyId}/{instance}")
	List<MessageDTO> getMessages(
		@PathVariable(name = "municipalityId") String municipalityId,
		@PathVariable(name = "familyId") String familyId, @PathVariable(name = "instance") String instance);

	@DeleteMapping("/{municipalityId}/messages")
	void deleteMessages(@PathVariable(name = "municipalityId") String municipalityId, List<Integer> ids);

	@GetMapping("/{municipalityId}/messages/attachments/{attachmentId}")
	byte[] getAttachment(@PathVariable(name = "municipalityId") String municipalityId, @PathVariable(name = "attachmentId") int attachmentId);

	@DeleteMapping("/{municipalityId}/messages/attachments/{attachmentId}")
	void deleteAttachment(@PathVariable(name = "municipalityId") String municipalityId, @PathVariable(name = "attachmentId") int attachmentId);

}
