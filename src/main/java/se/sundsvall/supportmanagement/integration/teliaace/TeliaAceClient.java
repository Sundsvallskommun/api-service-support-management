package se.sundsvall.supportmanagement.integration.teliaace;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.supportmanagement.integration.teliaace.configuration.TeliaAceConfiguration;

import static se.sundsvall.supportmanagement.integration.teliaace.configuration.TeliaAceConfiguration.CLIENT_ID;

/**
 * Client for Telia ACE's WorkItemInterfaceProxy REST API. {@code url} is expected to already contain the base URI
 * including the system-id path segment, e.g. {@code https://wiproxy.ccs.teliacompany.net/wi-interface-proxy/rest/
 * <system>}.
 */
@FeignClient(name = CLIENT_ID, url = "${integration.telia-ace.url}", configuration = TeliaAceConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface TeliaAceClient {

	@PostMapping("/workitem")
	AddWorkItemResponse addWorkItem(@RequestBody AddWorkItemRequest request);
}
