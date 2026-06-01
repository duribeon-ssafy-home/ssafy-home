package com.ssafy.home.auth.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.type.Role;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	private final JwtProperties properties;
	private final ObjectMapper objectMapper;

	public JwtTokenProvider(JwtProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public String createAccessToken(User user) {
		Instant now = Instant.now();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("sub", user.getId().toString());
		payload.put("email", user.getEmail());
		payload.put("role", user.getRole().name());
		payload.put("iat", now.getEpochSecond());
		payload.put("exp", now.plusSeconds(properties.accessTokenExpirationSeconds()).getEpochSecond());

		return encode(payload);
	}

	public JwtAuthentication parse(String token) {
		try {
			String[] chunks = token.split("\\.");
			if (chunks.length != 3 || !isValidSignature(chunks)) {
				throw new BusinessException(ErrorCode.INVALID_TOKEN);
			}

			Map<String, Object> payload = objectMapper.readValue(
					BASE64_URL_DECODER.decode(chunks[1]),
					new TypeReference<>() {
					}
			);

			long expiresAt = ((Number) payload.get("exp")).longValue();
			if (Instant.now().getEpochSecond() >= expiresAt) {
				throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
			}

			return new JwtAuthentication(
					Long.valueOf((String) payload.get("sub")),
					(String) payload.get("email"),
					Role.valueOf((String) payload.get("role"))
			);
		} catch (BusinessException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
	}

	public long getAccessTokenExpirationSeconds() {
		return properties.accessTokenExpirationSeconds();
	}

	private String encode(Map<String, Object> payload) {
		try {
			Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
			String encodedHeader = BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(header));
			String encodedPayload = BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
			String signature = sign(encodedHeader + "." + encodedPayload);
			return encodedHeader + "." + encodedPayload + "." + signature;
		} catch (Exception exception) {
			throw new IllegalStateException("JWT 생성에 실패했습니다.", exception);
		}
	}

	private boolean isValidSignature(String[] chunks) {
		String expected = sign(chunks[0] + "." + chunks[1]);
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), chunks[2].getBytes(StandardCharsets.UTF_8));
	}

	private String sign(String data) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
			return BASE64_URL_ENCODER.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("JWT 서명에 실패했습니다.", exception);
		}
	}
}
