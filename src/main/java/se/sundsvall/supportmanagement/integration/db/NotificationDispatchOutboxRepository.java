package se.sundsvall.supportmanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import se.sundsvall.supportmanagement.integration.db.model.NotificationDispatchOutboxEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;

@CircuitBreaker(name = "notificationDispatchOutboxRepository")
public interface NotificationDispatchOutboxRepository extends JpaRepository<NotificationDispatchOutboxEntity, String> {

	@Query("select distinct e.subscriber.id from NotificationDispatchOutboxEntity e where e.channel = :channel")
	List<String> findDistinctSubscriberIdsByChannel(NotificationChannelType channel);

	@EntityGraph(attributePaths = {
		"subscriber", "events"
	})
	List<NotificationDispatchOutboxEntity> findBySubscriberIdAndChannelOrderByCreatedAsc(String subscriberId, NotificationChannelType channel);
}
