package com.example.ssms.security.crypto;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class Argon2PasswordEncoder implements PasswordEncoder {

	private static final int ITERATIONS = 3;
	private static final int MEM_LIMIT = 65536; // 64 MB
	private static final int PARALLELISM = 1;
	private static final int HASH_LENGTH = 32;
	private static final int SALT_LENGTH = 16;

	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public String encode(CharSequence rawPassword) {
		byte[] salt = new byte[SALT_LENGTH];
		secureRandom.nextBytes(salt);

		Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
				.withVersion(Argon2Parameters.ARGON2_VERSION_13).withIterations(ITERATIONS).withMemoryAsKB(MEM_LIMIT)
				.withParallelism(PARALLELISM).withSalt(salt).build();

		Argon2BytesGenerator generator = new Argon2BytesGenerator();
		generator.init(params);

		byte[] result = new byte[HASH_LENGTH];
		generator.generateBytes(rawPassword.toString().getBytes(StandardCharsets.UTF_8), result, 0, result.length);

		return String.format("$argon2id$v=%d$m=%d,t=%d,p=%d$%s$%s", Argon2Parameters.ARGON2_VERSION_13, MEM_LIMIT,
				ITERATIONS, PARALLELISM, Base64.getEncoder().withoutPadding().encodeToString(salt),
				Base64.getEncoder().withoutPadding().encodeToString(result));
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		if (encodedPassword == null || !encodedPassword.startsWith("$argon2id$")) {
			return false;
		}

		try {
			String[] parts = encodedPassword.split("\\$");
			if (parts.length != 6) {
				return false;
			}

			String[] paramsParts = parts[3].split(",");
			int m = Integer.parseInt(paramsParts[0].substring(2));
			int t = Integer.parseInt(paramsParts[1].substring(2));
			int p = Integer.parseInt(paramsParts[2].substring(2));

			byte[] salt = Base64.getDecoder().decode(parts[4]);
			byte[] expectedHash = Base64.getDecoder().decode(parts[5]);

			Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
					.withVersion(Argon2Parameters.ARGON2_VERSION_13).withIterations(t).withMemoryAsKB(m)
					.withParallelism(p).withSalt(salt).build();

			Argon2BytesGenerator generator = new Argon2BytesGenerator();
			generator.init(params);

			byte[] calculatedHash = new byte[expectedHash.length];
			generator.generateBytes(rawPassword.toString().getBytes(StandardCharsets.UTF_8), calculatedHash, 0,
					calculatedHash.length);

			return constantTimeEquals(expectedHash, calculatedHash);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean constantTimeEquals(byte[] a, byte[] b) {
		if (a.length != b.length) {
			return false;
		}
		int diff = 0;
		for (int i = 0; i < a.length; i++) {
			diff |= a[i] ^ b[i];
		}
		return diff == 0;
	}
}
