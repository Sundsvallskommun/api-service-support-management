package se.sundsvall.supportmanagement.service.mapper;

import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.identifier.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.IdentifierEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.dept44.support.Identifier.Type.PARTY_ID;

class IdentifierEmbeddableMapperTest {

	@Test
	void toIdentifierMapsFromEmbeddable() {
		final var dto = IdentifierEmbeddableMapper.toIdentifier(
			IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"));

		assertThat(dto).isEqualTo(Identifier.create().withType("adAccount").withValue("joe01doe"));
	}

	@Test
	void toIdentifierReturnsNullForNull() {
		assertThat(IdentifierEmbeddableMapper.toIdentifier(null)).isNull();
	}

	@Test
	void toIdentifierReturnsNullWhenBothFieldsAreNull() {
		assertThat(IdentifierEmbeddableMapper.toIdentifier(IdentifierEmbeddable.create())).isNull();
	}

	@Test
	void toIdentifierEmbeddableMapsFromDto() {
		final var emb = IdentifierEmbeddableMapper.toIdentifierEmbeddable(
			Identifier.create().withType("partyId").withValue("party-123"));

		assertThat(emb).isEqualTo(IdentifierEmbeddable.create().withType("partyId").withValue("party-123"));
	}

	@Test
	void toIdentifierEmbeddableReturnsNullForNull() {
		assertThat(IdentifierEmbeddableMapper.toIdentifierEmbeddable(null)).isNull();
	}

	@Test
	void fromExecutingUserMapsAdAccount() {
		final var emb = IdentifierEmbeddableMapper.fromExecutingUser(
			se.sundsvall.dept44.support.Identifier.create().withType(AD_ACCOUNT).withValue("joe01doe"));

		assertThat(emb).isEqualTo(IdentifierEmbeddable.create().withType("adAccount").withValue("joe01doe"));
	}

	@Test
	void fromExecutingUserMapsPartyId() {
		final var emb = IdentifierEmbeddableMapper.fromExecutingUser(
			se.sundsvall.dept44.support.Identifier.create().withType(PARTY_ID).withValue("98c7b451-a14a-4f9f-91da-8834ba01eb81"));

		assertThat(emb).isEqualTo(IdentifierEmbeddable.create().withType("partyId").withValue("98c7b451-a14a-4f9f-91da-8834ba01eb81"));
	}

	@Test
	void fromExecutingUserReturnsNullForNull() {
		assertThat(IdentifierEmbeddableMapper.fromExecutingUser(null)).isNull();
	}

	@Test
	void fromExecutingUserReturnsNullWhenTypeMissing() {
		assertThat(IdentifierEmbeddableMapper.fromExecutingUser(
			se.sundsvall.dept44.support.Identifier.create().withValue("joe01doe"))).isNull();
	}
}
