package pe.edu.utp.medifast.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordEncoderTest {

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

	@Test
	void aceptaLaContrasenaDelSeed() {
		String hashDelSeed = "$2a$10$Kp2Urj3xvFRlV6JkzAALN.03p/dIZstkzKJAbQ3ovmtqLPTkOMzDm";

		assertThat(passwordEncoder.matches("Medifast2026", hashDelSeed)).isTrue();
	}

	@Test
	void generaHashDeSesentaCaracteresConCosteDiez() {
		String hash = passwordEncoder.encode("Medifast2026");

		assertThat(hash).hasSize(60).startsWith("$2a$10$");
	}
}
