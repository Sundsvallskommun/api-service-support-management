package se.sundsvall.supportmanagement.service;

import static java.util.Collections.emptyList;

import generated.se.sundsvall.employee.Employee;
import generated.se.sundsvall.employee.PortalPersonData;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.stereotype.Service;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.employee.EmployeeClient;

@Service
public class EmployeeService {

	private static final String DOMAIN_PERSONAL = "personal";

	private final EmployeeClient employeeClient;

	public EmployeeService(final EmployeeClient employeeClient) {
		this.employeeClient = employeeClient;
	}

	public PortalPersonData getEmployeeByLoginName(final String loginName) {
		return employeeClient.getEmployeeByDomainAndLoginName(DOMAIN_PERSONAL, loginName)
			.orElse(null);
	}

	public Employee getEmployeeByPartyId(final StakeholderEntity stakeholderEntity) {
		return Optional.ofNullable(stakeholderEntity)
			.flatMap(stakeholder -> {
				final var filter = URLEncoder.encode("{\"PersonId\": \"" + stakeholder.getExternalId() + "\"}", StandardCharsets.UTF_8);
				return employeeClient.getEmployeeInformation(filter)
					.orElse(emptyList())
					.stream()
					.findFirst();
			})
			.orElse(null);
	}

}
