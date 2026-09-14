package se.sundsvall.supportmanagement.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import static se.sundsvall.supportmanagement.service.util.ServiceUtil.TRIGGER_PROCESS_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearTriggerProcess;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.setTriggerProcess;

/**
 * Carries the caller's answer to whether the write should wake the process of the errand it touches.
 * <p>
 * Held for the request rather than passed through every signature that leads to publication, in the same way the
 * request group id already is. Nothing is echoed back on the response: the value says what the caller wanted, not what
 * happened, and publication is free to disregard it - it is not honoured for ad accounts.
 * <p>
 * A write with no request at all, a scheduled job, leaves the value unset and wakes the process.
 */
@Component
public class TriggerProcessFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
		throws ServletException, IOException {

		setTriggerProcess(request.getHeader(TRIGGER_PROCESS_HEADER));
		try {
			filterChain.doFilter(request, response);
		} finally {
			clearTriggerProcess();
		}
	}
}
