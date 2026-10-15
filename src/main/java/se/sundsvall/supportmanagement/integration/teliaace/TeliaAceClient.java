package se.sundsvall.supportmanagement.integration.teliaace;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.supportmanagement.integration.teliaace.configuration.TeliaAceConfiguration;

import static se.sundsvall.supportmanagement.integration.teliaace.configuration.TeliaAceConfiguration.CLIENT_ID;

// Defaulted, not required like other integrations' urls: the feature is opt-in per namespace, so a namespace that
// never turns it on must not be able to keep the whole service from starting in an environment where it isn't
// configured yet. The default resolves to nowhere real, so an unconfigured environment fails calls, not startup.
@FeignClient(name = CLIENT_ID, url = "${integration.telia-ace.url:https://telia-ace.invalid}", configuration = TeliaAceConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface TeliaAceClient {

	// Telia's own example posts to ".../workitem/" with a trailing slash, unlike the spec's path table. Matching it
	// exactly: a request without the slash got a 403 from their gateway with no other apparent cause.
	@PostMapping("/workitem/")
	AddWorkItemResponse addWorkItem(@RequestBody AddWorkItemRequest request);

	@PutMapping("/workitem/emailUid/{emailUid}")
	AddWorkItemResponse updateContactData(@PathVariable("emailUid") String emailUid, @RequestBody UpdateContactDataRequest request);
}
