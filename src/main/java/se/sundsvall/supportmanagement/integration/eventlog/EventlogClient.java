package se.sundsvall.supportmanagement.integration.eventlog;

import feign.QueryMap;
import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.PageEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import se.sundsvall.supportmanagement.integration.eventlog.configuration.EventlogConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.integration.eventlog.configuration.EventlogConfiguration.CLIENT_ID;

@FeignClient(name = CLIENT_ID, url = "${integration.eventlog.url}", configuration = EventlogConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface EventlogClient {

	/**
	 * Fetch a single log event by its own id.
	 *
	 * @param  municipalityId municipality id the event belongs to
	 * @param  id             the unique id of the event
	 * @return                the event
	 */
	@GetMapping(path = "/{municipalityId}/events/{id}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Event> getEvent(
		@PathVariable String municipalityId,
		@PathVariable String id);

	/**
	 * Create a log event under logKey.
	 *
	 * @param municipalityId municipality id to create event for
	 * @param logKey         containing UUID to create event for
	 * @param event          the event to create
	 */
	@PostMapping(path = "/{municipalityId}/{logKey}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createEvent(
		@PathVariable String municipalityId,
		@PathVariable String logKey,
		@RequestBody Event event);

	/**
	 * Fetch created log events for a logKey.
	 *
	 * @param  municipalityId municipality id to fetch events for
	 * @param  logKey         containing UUID to fetch events for
	 * @param  pageable       information of page, size and sorting options for the request
	 * @param  requestGroupId optional filter to only return events belonging to a specific request group
	 * @return                response containing result of search based on the provided parameters
	 */
	@GetMapping(path = "/{municipalityId}/{logKey}", produces = APPLICATION_JSON_VALUE)
	PageEvent getEvents(
		@PathVariable String municipalityId,
		@PathVariable String logKey,
		@QueryMap Pageable pageable,
		@RequestParam(value = "requestGroupId", required = false) String requestGroupId);
}
