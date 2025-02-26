package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.employee.PortalPersonData;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.employee.EmployeeClient;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	@Mock
	private EmployeeClient employeeClientMock;

	@InjectMocks
	private EmployeeService employeeService;

	@Test
	void getEmployeeByLoginName() {

		// Arrange
		final var municipalityId = "municipalityId";
		final var loginName = "loginName";
		final var domain = "personal";
		final var portalPersonData = new PortalPersonData();

		when(employeeClientMock.getEmployeeByDomainAndLoginName(municipalityId, domain, loginName)).thenReturn(Optional.of(portalPersonData));
		// Act
		final var result = employeeService.getEmployeeByLoginName(municipalityId, loginName);

		// Assert
		assertThat(result).isNotNull().isSameAs(portalPersonData);

		verify(employeeClientMock).getEmployeeByDomainAndLoginName(municipalityId, domain, loginName);
	}

	@Test
	void getEmployeeByLoginName_noEmployee() {

		// Arrange
		final var municipalityId = "municipalityId";
		final var loginName = "loginName";
		final var domain = "personal";

		when(employeeClientMock.getEmployeeByDomainAndLoginName(municipalityId, domain, loginName)).thenReturn(Optional.empty());
		// Act
		final var result = employeeService.getEmployeeByLoginName(municipalityId, loginName);

		// Assert
		assertThat(result).isNull();

		verify(employeeClientMock).getEmployeeByDomainAndLoginName(municipalityId, domain, loginName);
	}
}
