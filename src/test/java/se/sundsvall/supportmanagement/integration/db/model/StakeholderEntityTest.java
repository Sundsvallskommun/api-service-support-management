package se.sundsvall.supportmanagement.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

import static com.google.code.beanmatchers.BeanMatchers.*;
import static java.time.OffsetDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.assertj.core.api.Assertions.assertThat;

class StakeholderEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(StakeholderEntity.class, allOf(
				hasValidBeanConstructor(),
				hasValidGettersAndSetters(),
				hasValidBeanHashCode(),
				hasValidBeanEquals(),
				hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		var id = 1;
		var stakeholderId = "stakeholderId";
		var type = "type";
		var errand = ErrandEntity.create();
		var firstName = "firstName";
		var lastName = "lastName";
		var address = "address";
		var careOf = "careOf";
		var zipCode = "zipCode";
		var country = "country";
		var contactChannel = ContactChannelEntity.create();

		var stakeholderEntity = StakeholderEntity.create()
				.withId(id)
				.withStakeholderId(stakeholderId)
				.withType(type)
				.withErrandEntity(errand)
				.withFirstName(firstName)
				.withLastName(lastName)
				.withAddress(address)
				.withCareOf(careOf)
				.withZipCode(zipCode)
				.withCountry(country)
				.withContactChannels(List.of(contactChannel));

		assertThat(stakeholderEntity.getId()).isEqualTo(id);
		assertThat(stakeholderEntity.getStakeholderId()).isEqualTo(stakeholderId);
		assertThat(stakeholderEntity.getType()).isEqualTo(type);
		assertThat(stakeholderEntity.getErrandEntity()).isSameAs(errand);
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
		Assertions.assertThat(StakeholderEntity.create()).hasAllNullFieldsOrPropertiesExcept("id");
		Assertions.assertThat(new StakeholderEntity()).hasAllNullFieldsOrPropertiesExcept("id");
	}
}
