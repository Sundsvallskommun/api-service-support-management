package se.sundsvall.supportmanagement.integration.messaging;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.messaging.configuration.MessagingConfiguration.CLIENT_ID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.SmsRequest;
import se.sundsvall.supportmanagement.integration.messaging.configuration.MessagingConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.messaging.url}", configuration = MessagingConfiguration.class)
public interface MessagingClient {

	/**
	 * Send a single e-mail
	 * 
	 * @param sendAsynchronously how to send the message (true to send asynchronously, false to wait for response)
	 * @param emailRequest       containing email information
	 * @return response containing id for sent message
	 */
	@PostMapping(path = "/email", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageResult sendEmail(@RequestParam("async") boolean sendAsynchronously, @RequestBody EmailRequest emailRequest);

	/**
	 * Send a single sms
	 * 
	 * @param sendAsynchronously how to send the message (true to send asynchronously, false to wait for response)
	 * @param smsRequest         containing sms information
	 * @return response containing id for sent message
	 */
	@PostMapping(path = "/sms", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	MessageResult sendSms(@RequestParam("async") boolean sendAsynchronously, @RequestBody SmsRequest smsRequest);
}
