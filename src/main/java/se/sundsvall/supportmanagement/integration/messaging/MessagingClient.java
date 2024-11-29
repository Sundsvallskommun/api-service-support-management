package se.sundsvall.supportmanagement.integration.messaging;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.messaging.configuration.MessagingConfiguration.CLIENT_ID;

import generated.se.sundsvall.messaging.WebMessageRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import se.sundsvall.supportmanagement.integration.messaging.configuration.MessagingConfiguration;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.SmsRequest;

@FeignClient(name = CLIENT_ID, url = "${integration.messaging.url}", configuration = MessagingConfiguration.class)
public interface MessagingClient {

	/**
	 * Send a single e-mail
	 *
	 * @param  municipalityId     the id of the municipality to send the email to
	 * @param  sendAsynchronously how to send the message (true to send asynchronously, false to wait for response)
	 * @param  emailRequest       containing email information
	 * @return                    response containing id and delivery results for sent message
	 */
	@PostMapping(path = "/{municipalityId}/email", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageResult sendEmail(
		@PathVariable("municipalityId") final String municipalityId,
		@RequestParam("async") final boolean sendAsynchronously,
		@RequestBody final EmailRequest emailRequest);

	/**
	 * Send a single sms
	 *
	 * @param  municipalityId     the id of the municipality to send the email to
	 * @param  sendAsynchronously how to send the message (true to send asynchronously, false to wait for response)
	 * @param  smsRequest         containing sms information
	 * @return                    response containing id and delivery results for sent message
	 */
	@PostMapping(path = "/{municipalityId}/sms", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageResult sendSms(
		@PathVariable("municipalityId") final String municipalityId,
		@RequestParam("async") final boolean sendAsynchronously,
		@RequestBody final SmsRequest smsRequest);

	@PostMapping(path = "/{municipalityId}/webmessage", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageResult sendWebMessage(
		@PathVariable("municipalityId") final String municipalityId,
		@RequestParam("async") final boolean sendAsynchronously,
		@RequestBody final WebMessageRequest webMessageRequest);
}
