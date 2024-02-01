package se.sundsvall.supportmanagement.integration.webmessagecollector;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import se.sundsvall.supportmanagement.integration.webmessagecollector.configuration.WebMessageCollectorConfiguration;

import generated.se.sundsvall.webmessagecollector.MessageDTO;


@FeignClient(
	name = WebMessageCollectorConfiguration.CLIENT_ID,
	url = "${integration.web-message-collector.url}",
	configuration = WebMessageCollectorConfiguration.class
)
public interface WebMessageCollectorClient {

	@GetMapping("/messages")
	List<MessageDTO> getMessages(@RequestParam(name = "familyid") String familyId);


	@DeleteMapping("/messages")
	void deleteMessages(List<Integer> ids);

}
