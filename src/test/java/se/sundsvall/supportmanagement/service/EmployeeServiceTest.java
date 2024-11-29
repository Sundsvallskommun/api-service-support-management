package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.employee.EmployeeClient;

import generated.se.sundsvall.employee.Employee;
import generated.se.sundsvall.employee.PortalPersonData;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	@Mock
	private EmployeeClient employeeClientMock;

	@InjectMocks
	private EmployeeService employeeService;

	@Test
	void getEmployeeByLoginName() {

		// Arrange
		final var loginName = "loginName";
		final var domain = "personal";
		final var portalPersonData = new PortalPersonData();

		when(employeeClientMock.getEmployeeByDomainAndLoginName(domain, loginName)).thenReturn(Optional.of(portalPersonData));
		// Act
		final var result = employeeService.getEmployeeByLoginName(loginName);

		// Assert
		assertThat(result).isNotNull().isSameAs(portalPersonData);

		verify(employeeClientMock).getEmployeeByDomainAndLoginName(domain, loginName);
	}

	@Test
	void getEmployeeByLoginName_noEmployee() {

		// Arrange
		final var loginName = "loginName";
		final var domain = "personal";

		when(employeeClientMock.getEmployeeByDomainAndLoginName(domain, loginName)).thenReturn(Optional.empty());
		// Act
		final var result = employeeService.getEmployeeByLoginName(loginName);

		// Assert
		assertThat(result).isNull();

		verify(employeeClientMock).getEmployeeByDomainAndLoginName(domain, loginName);
	}

	@Test
	void getEmployeeByPartyId() {

		// Arrange
		final var loginName = "loginName";
		final var stakeholderRole = "ADMINISTRATOR";
		final var externalId = "partyId";

		final var errandEntity = new ErrandEntity().withStakeholders(List.of(StakeholderEntity.create().withExternalId(externalId).withRole(stakeholderRole)));
		final var employee = new Employee().loginname(loginName);

		when(employeeClientMock.getEmployeeInformation(any(String.class))).thenReturn(Optional.of(List.of(employee)));
		// Act
		final var result = employeeService.getEmployeeByPartyId(errandEntity.getStakeholders().getFirst());
		// Assert
		assertThat(result).isNotNull().isSameAs(employee);

		verify(employeeClientMock).getEmployeeInformation(any(String.class));
	}

	@Test
	void getEmployeeByPartyId_noEmployee() {

		// Arrange
		final var externalId = "partyId";
		final var stakeholderRole = "ADMINISTRATOR";
		final var errandEntity = new ErrandEntity().withStakeholders(List.of(StakeholderEntity.create().withExternalId(externalId).withRole(stakeholderRole)));
		when(employeeClientMock.getEmployeeInformation(any(String.class))).thenReturn(Optional.empty());
		// Act
		final var result = employeeService.getEmployeeByPartyId(errandEntity.getStakeholders().getFirst());
		// Assert
		assertThat(result).isNull();

		verify(employeeClientMock).getEmployeeInformation(any(String.class));
	}

}
