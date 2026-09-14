package se.sundsvall.supportmanagement.filter;

import jakarta.servlet.FilterChain;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.TRIGGER_PROCESS_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearTriggerProcess;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getTriggerProcess;

@ExtendWith(MockitoExtension.class)
class TriggerProcessFilterTest {

	@Mock
	private FilterChain filterChainMock;

	@InjectMocks
	private TriggerProcessFilter filter;

	@AfterEach
	void tearDown() {
		clearTriggerProcess();
	}

	@Test
	@DisplayName("Verification that the value is held while the request runs, exactly as it arrived, and let go afterwards")
	void theHeaderIsHeldForTheRequestAndLetGoAfterIt() throws Exception {
		final var request = new MockHttpServletRequest();
		request.addHeader(TRIGGER_PROCESS_HEADER, "  FALSE ");
		final var seen = new String[1];

		filter.doFilterInternal(request, new MockHttpServletResponse(), (_, _) -> seen[0] = getTriggerProcess());

		assertThat(seen[0]).isEqualTo("  FALSE ");
		assertThat(getTriggerProcess()).isNull();
	}

	@Test
	void aRequestWithoutTheHeaderHoldsNothing() throws Exception {
		final var seen = new String[1];

		filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), (_, _) -> seen[0] = getTriggerProcess());

		assertThat(seen[0]).isNull();
	}

	@Test
	@DisplayName("Verification that the value is let go even when the request fails, so that it cannot leak into the next request on the thread")
	void theValueIsLetGoWhenTheChainThrows() throws Exception {
		final var request = new MockHttpServletRequest();
		request.addHeader(TRIGGER_PROCESS_HEADER, "false");
		final var response = new MockHttpServletResponse();

		doThrow(new IOException("the request failed")).when(filterChainMock).doFilter(any(), any());

		assertThatExceptionOfType(IOException.class).isThrownBy(() -> filter.doFilterInternal(request, response, filterChainMock));

		assertThat(getTriggerProcess()).isNull();
		verify(filterChainMock).doFilter(request, response);
	}
}
