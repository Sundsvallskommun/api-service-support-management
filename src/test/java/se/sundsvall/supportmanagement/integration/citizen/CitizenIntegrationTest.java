package se.sundsvall.supportmanagement.integration.citizen;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.TestObjectsBuilder.createCitizenExtended;

@ExtendWith(MockitoExtension.class)
class CitizenIntegrationTest {

	@Mock
	private CitizenClient citizenClientMock;

	@InjectMocks
	private CitizenIntegration citizenIntegration;

	@Test
	void getCitizenName() {
		var municipalityId = "municipalityId";
		var partyId = UUID.randomUUID().toString();

		when(citizenClientMock.getPerson(municipalityId, partyId)).thenReturn(createCitizenExtended("Clark", "Kent"));

		var result = citizenIntegration.getCitizenName(municipalityId, partyId);

		assertThat(result).isEqualTo("Clark Kent");
		verify(citizenClientMock).getPerson(municipalityId, partyId);

	}

}
