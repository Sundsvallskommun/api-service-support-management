package se.sundsvall.supportmanagement.service;

import static java.util.Collections.emptyList;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.stereotype.Service;

import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.employee.EmployeeClient;

import generated.se.sundsvall.employee.Employee;
import generated.se.sundsvall.employee.PortalPersonData;

@Service
public class EmployeeService {

	private static final String DOMAIN_PERSONAL = "personal";

	private final EmployeeClient employeeClient;

	public EmployeeService(final EmployeeClient employeeClient) {this.employeeClient = employeeClient;}


	public PortalPersonData getEmployeeByLoginName(final String loginName) {
		return employeeClient.getEmployeeByDomainAndLoginName(DOMAIN_PERSONAL, loginName)
			.orElse(null);
	}

	public PortalPersonData getEmployeeByPartyId(final StakeholderEntity stakeholderEntity) {
		return Optional.ofNullable(stakeholderEntity)
			.flatMap(stakeholder ->
			{
				final var filter = URLEncoder.encode("{\"PersonId\": \"" + stakeholder.getExternalId() + "\"}", StandardCharsets.UTF_8);
				return employeeClient.getEmployeeInformation(filter)
					.orElse(emptyList())
					.stream()
					.findFirst()
					.map(Employee::getLoginname);
			})
			.map(this::getEmployeeByLoginName)
			.orElse(null);
	}

}
