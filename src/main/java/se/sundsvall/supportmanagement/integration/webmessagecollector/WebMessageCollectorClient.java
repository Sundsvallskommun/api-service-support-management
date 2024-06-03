package se.sundsvall.supportmanagement.integration.webmessagecollector;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import se.sundsvall.supportmanagement.integration.webmessagecollector.configuration.WebMessageCollectorConfiguration;

import generated.se.sundsvall.webmessagecollector.MessageDTO;


@FeignClient(
	name = WebMessageCollectorConfiguration.CLIENT_ID,
	url = "${integration.web-message-collector.url}",
	configuration = WebMessageCollectorConfiguration.class
)
public interface WebMessageCollectorClient {

	@GetMapping("/messages/{familyId}/{instance}")
	List<MessageDTO> getMessages(@PathVariable(name = "familyId") String familyId, @PathVariable(name = "instance") String instance);


	@DeleteMapping("/messages")
	void deleteMessages(List<Integer> ids);

	@GetMapping("/messages/attachments/{attachmentId}")
	byte[] getAttachment(@PathVariable(name = "attachmentId") int attachmentId);

	@DeleteMapping("/messages/attachments/{attachmentId}")
	void deleteAttachment(@PathVariable(name = "attachmentId") int attachmentId);

}
