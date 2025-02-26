package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.employee.PortalPersonData;
import org.springframework.stereotype.Service;
import se.sundsvall.supportmanagement.integration.employee.EmployeeClient;

@Service
public class EmployeeService {

	private static final String DOMAIN_PERSONAL = "personal";

	private final EmployeeClient employeeClient;

	public EmployeeService(final EmployeeClient employeeClient) {
		this.employeeClient = employeeClient;
	}

	public PortalPersonData getEmployeeByLoginName(final String municipalityId, final String loginName) {
		return employeeClient.getEmployeeByDomainAndLoginName(municipalityId, DOMAIN_PERSONAL, loginName)
			.orElse(null);
	}
}
