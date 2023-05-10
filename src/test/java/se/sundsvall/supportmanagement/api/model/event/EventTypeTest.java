package se.sundsvall.supportmanagement.api.model.event;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static se.sundsvall.supportmanagement.api.model.event.EventType.CREATE;
import static se.sundsvall.supportmanagement.api.model.event.EventType.DELETE;
import static se.sundsvall.supportmanagement.api.model.event.EventType.UNKNOWN;
import static se.sundsvall.supportmanagement.api.model.event.EventType.UPDATE;

import org.junit.jupiter.api.Test;

class EventTypeTest {

	@Test
	void enums() {
		assertThat(EventType.values()).containsExactlyInAnyOrder(CREATE, DELETE, UNKNOWN, UPDATE);
	}
}
