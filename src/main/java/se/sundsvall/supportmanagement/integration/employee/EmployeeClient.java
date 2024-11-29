package se.sundsvall.supportmanagement.integration.employee;

import static se.sundsvall.supportmanagement.integration.employee.configuration.EmployeeConfiguration.CLIENT_ID;

import java.util.List;
import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import se.sundsvall.supportmanagement.integration.employee.configuration.EmployeeConfiguration;

import generated.se.sundsvall.employee.Employee;
import generated.se.sundsvall.employee.PortalPersonData;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.employee.url}",
	configuration = EmployeeConfiguration.class,
	dismiss404 = true)
public interface EmployeeClient {

	/**
	 * Uses the employments endpoint which makes it possible to filter on more fields.
	 *
	 * @param  filter containing the filter
	 * @return        List of employees
	 */
	@GetMapping(path = "/employments?filter={filter}", produces = {
		MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE
	})
	Optional<List<Employee>> getEmployeeInformation(@PathVariable("filter") String filter);

	/**
	 * Get Userdata from the employee service by domain and loginName.
	 *
	 * @param  domain    domain of the employee
	 * @param  loginName login name of the employee
	 * @return           PortalPersonData with information about the employee
	 */
	@GetMapping(path = "/portalpersondata/{domain}/{loginName}", produces = {
		MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE
	})
	Optional<PortalPersonData> getEmployeeByDomainAndLoginName(@PathVariable("domain") String domain, @PathVariable("loginName") String loginName);

}
