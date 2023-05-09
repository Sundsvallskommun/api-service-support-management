package se.sundsvall.supportmanagement.integration.eventlog;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.eventlog.configuration.EventlogConfiguration.CLIENT_ID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.PageEvent;
import se.sundsvall.supportmanagement.integration.eventlog.configuration.EventlogConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.eventlog.url}", configuration = EventlogConfiguration.class)
public interface EventlogClient {

	/**
	 * Create a log event under logKey.
	 *
	 * @param logKey containing UUID to fetch events for
	 * @param event  the event to create
	 */
	@PostMapping(path = "/{logKey}", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	ResponseEntity<Void> createEvent(
		@PathVariable("logKey") String logKey,
		@RequestBody Event event);

	/**
	 * Fetch log events.
	 * 
	 * @param logKey containing UUID to fetch events for
	 * @param page   the page number to retrieve
	 * @param size   the amount of entries to be fetched for each page
	 * @return response containing result of search based on the provided parameters
	 */
	@GetMapping(path = "/{logKey}?sort=created", produces = APPLICATION_JSON_VALUE)
	PageEvent getEvents(
		@PathVariable("logKey") String logKey,
		@RequestParam(name = "page") int page,
		@RequestParam(name = "size") int size);
}
