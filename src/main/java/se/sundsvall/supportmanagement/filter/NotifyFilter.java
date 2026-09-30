package se.sundsvall.supportmanagement.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import static se.sundsvall.supportmanagement.service.util.ServiceUtil.NOTIFY_HEADER;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.clearNotify;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.setNotify;

/**
 * Keeps the {@value se.sundsvall.supportmanagement.service.util.ServiceUtil#NOTIFY_HEADER} header on the thread for
 * the length of the request, so that whatever the request does can ask whether it should notify anyone without the
 * value being passed along through every layer.
 */
@Component
public class NotifyFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
		throws ServletException, IOException {

		setNotify(request.getHeader(NOTIFY_HEADER));
		try {
			filterChain.doFilter(request, response);
		} finally {
			clearNotify();
		}
	}
}
