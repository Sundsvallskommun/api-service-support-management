package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.emailreader.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;

import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class EmailReaderSchedulerTest {

	@Mock
	private EmailReaderWorker emailReaderWorkerMock;
	@Mock
	private EmailProcessingHealthIndicator healthIndicatorMock;
	@Mock
	private EmailWorkerConfigEntity emailWorkerConfigEntityMock;
	@Mock
	private Email emailMock1;
	@Mock
	private Email emailMock2;

	@InjectMocks
	private EmailReaderScheduler emailReaderScheduler;

	@Test
	void getAndProcessEmails() {
		// ARRANGE
		when(emailReaderWorkerMock.getEnabledEmailConfigs()).thenReturn(Set.of(emailWorkerConfigEntityMock));
		when(emailReaderWorkerMock.getEmailsFromConfig(any())).thenReturn(List.of(emailMock1));
		// Act
		emailReaderScheduler.getAndProcessEmails();
		// Verify
		verify(healthIndicatorMock).resetErrors();
		verify(emailReaderWorkerMock).getEnabledEmailConfigs();
		verify(emailReaderWorkerMock).getEmailsFromConfig(same(emailWorkerConfigEntityMock));
		verify(emailReaderWorkerMock).processEmail(same(emailMock1), same(emailWorkerConfigEntityMock));
		verify(healthIndicatorMock).hasErrors();
		verify(healthIndicatorMock).setHealthy();
		verifyNoMoreInteractions(emailReaderWorkerMock, healthIndicatorMock);
	}

	@Test
	void getAndProcessEmailsThrowsException() {
		// ARRANGE
		when(emailReaderWorkerMock.getEnabledEmailConfigs()).thenReturn(Set.of(emailWorkerConfigEntityMock));
		when(emailReaderWorkerMock.getEmailsFromConfig(any())).thenReturn(List.of(emailMock1, emailMock2));
		doThrow(new RuntimeException("error")).when(emailReaderWorkerMock).processEmail(same(emailMock1), any());
		when(healthIndicatorMock.hasErrors()).thenReturn(true);
		// Act
		emailReaderScheduler.getAndProcessEmails();
		// Verify
		ArgumentCaptor<Email> emailArgumentCaptor = ArgumentCaptor.forClass(Email.class);
		verify(healthIndicatorMock).resetErrors();
		verify(emailReaderWorkerMock).getEnabledEmailConfigs();
		verify(emailReaderWorkerMock).getEmailsFromConfig(same(emailWorkerConfigEntityMock));
		verify(emailReaderWorkerMock, times(2)).processEmail(emailArgumentCaptor.capture(), same(emailWorkerConfigEntityMock));
		verify(healthIndicatorMock).setUnhealthy();
		verify(healthIndicatorMock).hasErrors();
		assertThat(emailArgumentCaptor.getAllValues()).containsExactly(emailMock1, emailMock2);
		verifyNoMoreInteractions(emailReaderWorkerMock, healthIndicatorMock);
	}

}
