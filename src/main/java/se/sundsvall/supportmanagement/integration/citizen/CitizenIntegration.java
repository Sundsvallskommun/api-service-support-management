package se.sundsvall.supportmanagement.integration.citizen;

import org.springframework.stereotype.Component;

@Component
public class CitizenIntegration {

	private final CitizenClient citizenClient;

	public CitizenIntegration(final CitizenClient citizenClient) {
		this.citizenClient = citizenClient;
	}

	public String getCitizenName(final String municipalityId, final String partyId) {
		var citizen = citizenClient.getPerson(municipalityId, partyId);
		var firstName = citizen.getGivenname();
		var lastName = citizen.getLastname();

		return "%s %s".formatted(firstName, lastName);
	}

}
