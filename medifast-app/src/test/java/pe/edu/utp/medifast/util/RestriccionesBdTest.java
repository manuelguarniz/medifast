package pe.edu.utp.medifast.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class RestriccionesBdTest {

	@Test
	void encuentraLaRestriccionEnCualquierCausa() {
		DataIntegrityViolationException ex = new DataIntegrityViolationException("could not execute statement",
				new RuntimeException("constraint", new SQLException("Duplicate entry for key 'UQ_PACIENTE_CORREO'")));

		assertThat(RestriccionesBd.viola(ex, "uq_paciente_correo")).isTrue();
		assertThat(RestriccionesBd.viola(ex, "uq_usuario_documento")).isFalse();
	}

	@Test
	void toleraCausasSinMensaje() {
		assertThat(RestriccionesBd.viola(new DataIntegrityViolationException(null), "uq_cita_horario_activo"))
				.isFalse();
	}
}
