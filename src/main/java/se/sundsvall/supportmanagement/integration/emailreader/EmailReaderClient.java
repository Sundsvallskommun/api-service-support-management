package se.sundsvall.supportmanagement.integration.emailreader;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.emailreader.configuration.EmailreaderConfiguration.CLIENT_ID;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import se.sundsvall.supportmanagement.integration.emailreader.configuration.EmailreaderConfiguration;

import generated.se.sundsvall.emailreader.Email;

@FeignClient(name = CLIENT_ID, url = "${integration.emailreader.url}", configuration = EmailreaderConfiguration.class)
public interface EmailReaderClient {

	@GetMapping(path = "/email", produces = APPLICATION_JSON_VALUE)
	List<Email> getEmails();

}
