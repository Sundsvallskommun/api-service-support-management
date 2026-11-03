package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import generated.se.sundsvall.emailreader.Email;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailReaderSchedulerTest {

	private static final String JOB_NAME = "emailreader";

	@Mock
	private EmailReaderWorker emailReaderWorkerMock;
	@Mock
	private Dept44HealthUtility healthIndicatorMock;
	@Mock
	private EmailWorkerConfigEntity emailWorkerConfigEntityMock;
	@Mock
	private Email emailMock1;
	@Mock
	private Email emailMock2;
	private EmailReaderScheduler emailReaderScheduler;

	@BeforeEach
	void setUp() {
		emailReaderScheduler = new EmailReaderScheduler(emailReaderWorkerMock, healthIndicatorMock, JOB_NAME);
	}

	@Test
	void getAndProcessEmails() {
		// ARRANGE
		when(emailReaderWorkerMock.getEnabledEmailConfigs()).thenReturn(Set.of(emailWorkerConfigEntityMock));
		when(emailReaderWorkerMock.getEmailsFromConfig(any())).thenReturn(List.of(emailMock1));
		// Act
		emailReaderScheduler.getAndProcessEmails();
		// Verify
		verify(emailReaderWorkerMock).getEnabledEmailConfigs();
		verify(emailReaderWorkerMock).getEmailsFromConfig(same(emailWorkerConfigEntityMock));
		verify(emailReaderWorkerMock).processEmail(same(emailMock1), same(emailWorkerConfigEntityMock), any());
		verifyNoMoreInteractions(emailReaderWorkerMock);
		verifyNoInteractions(healthIndicatorMock);
	}

	@Test
	void getAndProcessEmailsThrowsException() {
		// ARRANGE
		when(emailReaderWorkerMock.getEnabledEmailConfigs()).thenReturn(Set.of(emailWorkerConfigEntityMock));
		when(emailReaderWorkerMock.getEmailsFromConfig(any())).thenReturn(List.of(emailMock1, emailMock2));

		doThrow(new RuntimeException("error")).when(emailReaderWorkerMock).processEmail(same(emailMock1), any(), any());
		// Act
		emailReaderScheduler.getAndProcessEmails();
		// Verify
		final ArgumentCaptor<Email> emailArgumentCaptor = ArgumentCaptor.forClass(Email.class);
		verify(emailReaderWorkerMock).getEnabledEmailConfigs();
		verify(emailReaderWorkerMock).getEmailsFromConfig(same(emailWorkerConfigEntityMock));
		verify(emailReaderWorkerMock, times(2)).processEmail(emailArgumentCaptor.capture(), same(emailWorkerConfigEntityMock), any());
		assertThat(emailArgumentCaptor.getAllValues()).containsExactly(emailMock1, emailMock2);
		verify(healthIndicatorMock).setHealthIndicatorUnhealthy(JOB_NAME, "Email error: Error processing email");
		verifyNoMoreInteractions(emailReaderWorkerMock, healthIndicatorMock);
	}
}
