package se.sundsvall.supportmanagement.integration.webmessagesender;

import generated.se.sundsvall.webmessagesender.CreateWebMessageRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.supportmanagement.integration.webmessagesender.configuration.WebMessageSenderConfiguration;

@FeignClient(
	name = WebMessageSenderConfiguration.CLIENT_ID,
	url = "${integration.web-message-sender.url}",
	configuration = WebMessageSenderConfiguration.class
)
public interface WebMessageSenderClient {

	@PostMapping("/{municipalityId}/webmessages")
	ResponseEntity<Void> sendMessage(
		@PathVariable(name = "municipalityId") String municipalityId,
		@RequestBody CreateWebMessageRequest request);
}
