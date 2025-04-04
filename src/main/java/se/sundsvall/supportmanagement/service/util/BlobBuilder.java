package se.sundsvall.supportmanagement.service.util;

import jakarta.persistence.EntityManager;
import java.sql.Blob;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Component
public class BlobBuilder {

	private final EntityManager entityManager;

	public BlobBuilder(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Blob createBlob(final byte[] content) {
		final var session = entityManager.unwrap(Session.class);
		return session.getLobHelper().createBlob(content);
	}

}
