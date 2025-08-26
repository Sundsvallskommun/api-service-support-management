package se.sundsvall.supportmanagement.integration.messagingsettings;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.messagingsettings.configuration.MessagingSettingsConfiguration.CLIENT_ID;

import generated.se.sundsvall.messagingsettings.SenderInfoResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import se.sundsvall.supportmanagement.integration.messagingsettings.configuration.MessagingSettingsConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.messaging-settings.url}", configuration = MessagingSettingsConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface MessagingSettingsClient {

	@GetMapping(path = "/{municipalityId}/{namespace}/{departmentId}/sender-info", produces = APPLICATION_JSON_VALUE)
	SenderInfoResponse getSenderInfo(
		@PathVariable(name = "municipalityId") final String municipalityId,
		@PathVariable(name = "namespace") final String namespace,
		@PathVariable(name = "departmentId") final String departmentId);

}
