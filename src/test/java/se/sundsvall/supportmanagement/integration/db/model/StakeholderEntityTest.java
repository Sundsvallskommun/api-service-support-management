package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.util.List;

import org.junit.jupiter.api.Test;

class StakeholderEntityTest {

	@Test
	void testBean() {
		assertThat(StakeholderEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = 1;
		final var externalId = "externalId";
		final var externalIdType = "externalIdTypeTag";
		final var errand = ErrandEntity.create();
		final var firstName = "firstName";
		final var lastName = "lastName";
		final var address = "address";
		final var careOf = "careOf";
		final var zipCode = "zipCode";
		final var country = "country";
		final var role = "role";
		final var contactChannel = ContactChannelEntity.create();

		final var stakeholderEntity = StakeholderEntity.create()
			.withId(id)
			.withExternalId(externalId)
			.withExternalIdType(externalIdType)
			.withErrandEntity(errand)
			.withRole(role)
			.withFirstName(firstName)
			.withLastName(lastName)
			.withAddress(address)
			.withCareOf(careOf)
			.withZipCode(zipCode)
			.withCountry(country)
			.withContactChannels(List.of(contactChannel));

		assertThat(stakeholderEntity.getId()).isEqualTo(id);
		assertThat(stakeholderEntity.getExternalId()).isEqualTo(externalId);
		assertThat(stakeholderEntity.getExternalIdType()).isEqualTo(externalIdType);
		assertThat(stakeholderEntity.getErrandEntity()).isSameAs(errand);
		assertThat(stakeholderEntity.getRole()).isEqualTo(role);
		assertThat(stakeholderEntity.getFirstName()).isEqualTo(firstName);
		assertThat(stakeholderEntity.getLastName()).isEqualTo(lastName);
		assertThat(stakeholderEntity.getAddress()).isEqualTo(address);
		assertThat(stakeholderEntity.getCareOf()).isEqualTo(careOf);
		assertThat(stakeholderEntity.getZipCode()).isEqualTo(zipCode);
		assertThat(stakeholderEntity.getCountry()).isEqualTo(country);
		assertThat(stakeholderEntity.getContactChannels()).containsExactly(contactChannel);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(StakeholderEntity.create()).hasAllNullFieldsOrPropertiesExcept("id");
		assertThat(new StakeholderEntity()).hasAllNullFieldsOrPropertiesExcept("id");
	}
}
