package se.sundsvall.supportmanagement.api.model.errand;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

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
		var externalId = "id";
		var externalIdTypeTag = "EMPLOYEE";
		var firstName = "firstName";
		var lastName = "lastName";
		var address = "address";
		var careOf = "careOf";
		var zipCode = "zipCode";
		var country = "country";
		var contactChannel = ContactChannel.create();

		var bean = Stakeholder.create()
			.withExternalId(externalId)
			.withExternalIdType(externalIdType)
			.withRole(role)
			.withFirstName(firstName)
			.withLastName(lastName)
			.withAddress(address)
			.withCareOf(careOf)
			.withZipCode(zipCode)
			.withCountry(country)
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
		assertThat(bean.getContactChannels()).containsExactly(contactChannel);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Stakeholder.create()).hasAllNullFieldsOrProperties();
		assertThat(new Stakeholder()).hasAllNullFieldsOrProperties();
	}
}
