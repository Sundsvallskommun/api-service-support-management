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
 * The value of the header is held for the duration of the request and cleared afterwards. Nothing is echoed back on
 * the response. Publication may disregard the value, and does for ad accounts.
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
