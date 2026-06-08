package com.ssafy.home.auth.jwt;

import com.ssafy.home.common.security.CustomUserDetails;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String header = request.getHeader(AUTHORIZATION);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			try {
				authenticate(header.substring(BEARER_PREFIX.length()));
			} catch (RuntimeException exception) {
				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(String token) {
		JwtAuthentication jwtAuthentication = jwtTokenProvider.parse(token);
		User user = userRepository.findById(jwtAuthentication.userId()).orElse(null);
		if (user == null || user.getStatus() != UserStatus.ACTIVE) {
			return;
		}

		CustomUserDetails userDetails = new CustomUserDetails(user);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				userDetails,
				null,
				userDetails.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
