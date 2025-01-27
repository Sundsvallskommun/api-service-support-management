package se.sundsvall.supportmanagement.integration.emailreader;

import static se.sundsvall.supportmanagement.integration.emailreader.configuration.EmailReaderConfiguration.CLIENT_ID;

import generated.se.sundsvall.emailreader.Email;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import se.sundsvall.supportmanagement.integration.emailreader.configuration.EmailReaderConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.emailreader.url}", configuration = EmailReaderConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface EmailReaderClient {

	@GetMapping("/{municipalityId}/email/{namespace}")
	List<Email> getEmails(@PathVariable("municipalityId") final String municipalityId, @PathVariable("namespace") final String namespace);

	@GetMapping("/{municipalityId}/email/attachments/{attachmentId}")
	byte[] getAttachment(@PathVariable("municipalityId") final String municipalityId, @PathVariable("attachmentId") final int attachmentId);

	@DeleteMapping("/{municipalityId}/email/{id}")
	void deleteEmail(@PathVariable("municipalityId") final String municipalityId, @PathVariable("id") final String id);
}
