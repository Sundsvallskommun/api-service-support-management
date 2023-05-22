package se.sundsvall.supportmanagement.api.filter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static se.sundsvall.supportmanagement.Constants.AD_USER_HEADER_KEY;

@Component
public class ExecutingUserSupplier extends OncePerRequestFilter {
	private static final String UNKNOWN = "UNKNOWN";

	private static final ThreadLocal<String> THREAD_LOCAL_AD_USER = new ThreadLocal<>();

	public String getAdUser() {
		return THREAD_LOCAL_AD_USER.get();
	}

	@Override
	protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		// Extract AD-user from ad-user header
		extractAdUser(request);

		try {
			filterChain.doFilter(request, response);
		} finally {
			// Remove value from threadlocal when filter chain has been processed
			THREAD_LOCAL_AD_USER.remove();
		}
	}

	void extractAdUser(HttpServletRequest request) {
		var headerValue = request.getHeader(AD_USER_HEADER_KEY);

		if (isBlank(headerValue)) {
			THREAD_LOCAL_AD_USER.set(UNKNOWN);
		} else {
			THREAD_LOCAL_AD_USER.set(headerValue);
		}
	}
}