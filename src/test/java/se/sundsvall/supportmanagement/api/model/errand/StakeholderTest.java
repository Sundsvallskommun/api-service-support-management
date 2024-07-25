package se.sundsvall.supportmanagement.api.model.errand;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class StakeholderTest {

	@Test
	void testBean() {
		assertThat(Stakeholder.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var externalId = "id";
		final var externalIdType = "EMPLOYEE";
		final var firstName = "firstName";
		final var lastName = "lastName";
		final var address = "address";
		final var careOf = "careOf";
		final var zipCode = "zipCode";
		final var country = "country";
		final var role = "role";
		final var city = "city";
		final var organizationName = "organizationName";
		final var contactChannel = ContactChannel.create();

		final var bean = Stakeholder.create()
			.withExternalId(externalId)
			.withExternalIdType(externalIdType)
			.withRole(role)
			.withFirstName(firstName)
			.withLastName(lastName)
			.withAddress(address)
			.withCareOf(careOf)
			.withZipCode(zipCode)
			.withCountry(country)
			.withCity(city)
			.withOrganizationName(organizationName)
			.withContactChannels(List.of(contactChannel));

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getExternalId()).isEqualTo(externalId);
		assertThat(bean.getExternalIdType()).isEqualTo(externalIdType);
		assertThat(bean.getRole()).isEqualTo(role);
		assertThat(bean.getFirstName()).isEqualTo(firstName);
		assertThat(bean.getLastName()).isEqualTo(lastName);
		assertThat(bean.getAddress()).isEqualTo(address);
		assertThat(bean.getCareOf()).isEqualTo(careOf);
		assertThat(bean.getZipCode()).isEqualTo(zipCode);
		assertThat(bean.getCountry()).isEqualTo(country);
		assertThat(bean.getCity()).isEqualTo(city);
		assertThat(bean.getOrganizationName()).isEqualTo(organizationName);
		assertThat(bean.getContactChannels()).containsExactly(contactChannel);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Stakeholder.create()).hasAllNullFieldsOrProperties();
		assertThat(new Stakeholder()).hasAllNullFieldsOrProperties();
	}
}
