package se.sundsvall.supportmanagement.service.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.util.Base64;

import jakarta.persistence.EntityManager;

import org.hibernate.Session;
import org.springframework.stereotype.Component;


@Component
public class BlobBuilder {

	private static final Base64.Decoder DECODER = Base64.getDecoder();

	private final EntityManager entityManager;

	public BlobBuilder(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Blob createBlob(final String base64content) {
		final var session = entityManager.unwrap(Session.class);
		final var decodedBytes = DECODER.decode(base64content.getBytes(UTF_8));
		final var stream = new ByteArrayInputStream(decodedBytes);

		return session.getLobHelper().createBlob(stream, decodedBytes.length);
	}


	public Blob createBlob(final byte[] content) {
		final var session = entityManager.unwrap(Session.class);
		return session.getLobHelper().createBlob(content);
	}

}
