package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import generated.se.sundsvall.emailreader.Email;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailReaderSchedulerTest {

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
	@Mock
	private Consumer<String> consumerMock;
	@InjectMocks
	private EmailReaderScheduler emailReaderScheduler;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(emailReaderScheduler, "jobName", "emailreader");
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
		verifyNoMoreInteractions(emailReaderWorkerMock, consumerMock);
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
		verifyNoMoreInteractions(emailReaderWorkerMock, consumerMock);
	}

}
