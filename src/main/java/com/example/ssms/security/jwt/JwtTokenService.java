package com.example.ssms.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

@Service
public class JwtTokenService {

	private final RSAPublicKey publicKey;
	private final RSAPrivateKey privateKey;
	private final String keyId;

	@Value("${ssms.security.jwt.issuer:ssms-api}")
	private String issuer;

	@Value("${ssms.security.jwt.access-token-expiration-seconds:900}")
	private long accessTokenExpirationSeconds;

	public JwtTokenService() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			KeyPair keyPair = keyGen.generateKeyPair();
			this.publicKey = (RSAPublicKey) keyPair.getPublic();
			this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
			this.keyId = UUID.randomUUID().toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Failed to initialize RSA key generator", e);
		}
	}

	public String generateAccessToken(UUID userId, String role, int permVersion, UUID sessionId) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(accessTokenExpirationSeconds);

		return Jwts.builder().header().keyId(keyId).and().subject(userId.toString()).claim("role", role)
				.claim("permVersion", permVersion).claim("sid", sessionId.toString()).issuer(issuer).audience()
				.add("ssms-clients").and().issuedAt(Date.from(now)).expiration(Date.from(exp))
				.signWith(privateKey, Jwts.SIG.RS256).compact();
	}

	public Claims parseAndValidateToken(String token) {
		return Jwts.parser().verifyWith(publicKey).requireIssuer(issuer).build().parseSignedClaims(token).getPayload();
	}

	public Map<String, Object> getJwks() {
		Map<String, Object> jwk = new LinkedHashMap<>();
		jwk.put("kty", "RSA");
		jwk.put("alg", "RS256");
		jwk.put("use", "sig");
		jwk.put("kid", keyId);
		jwk.put("n", Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getModulus().toByteArray()));
		jwk.put("e",
				Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getPublicExponent().toByteArray()));

		return Map.of("keys", List.of(jwk));
	}

	public long getAccessTokenExpirationSeconds() {
		return accessTokenExpirationSeconds;
	}
}
