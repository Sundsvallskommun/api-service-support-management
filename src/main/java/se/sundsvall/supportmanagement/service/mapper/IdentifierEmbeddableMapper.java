package se.sundsvall.supportmanagement.service.mapper;

import java.util.Optional;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;

/**
 * Bidirectional mapping between {@link Identifier} (API DTO), {@link IdentifierEmbeddable} (entity)
 * and the dept44 {@link se.sundsvall.dept44.support.Identifier} (executing-user context).
 *
 * Shared by both subscriber and subscription flows since both persist a createdBy identifier
 * derived from the request principal.
 */
public final class IdentifierEmbeddableMapper {

	private IdentifierEmbeddableMapper() {
		// Intentionally empty
	}

	public static Identifier toIdentifier(final IdentifierEmbeddable embeddable) {
		return Optional.ofNullable(embeddable)
			.filter(e -> e.getType() != null || e.getValue() != null)
			.map(e -> Identifier.create()
				.withType(e.getType())
				.withValue(e.getValue()))
			.orElse(null);
	}

	public static IdentifierEmbeddable toIdentifierEmbeddable(final Identifier dto) {
		return Optional.ofNullable(dto)
			.map(d -> IdentifierEmbeddable.create()
				.withType(d.getType())
				.withValue(d.getValue()))
			.orElse(null);
	}

	public static IdentifierEmbeddable fromExecutingUser(final se.sundsvall.dept44.support.Identifier identifier) {
		return Optional.ofNullable(identifier)
			.filter(i -> i.getTypeString() != null)
			.map(i -> IdentifierEmbeddable.create()
				.withType(i.getTypeString())
				.withValue(i.getValue()))
			.orElse(null);
	}
}
