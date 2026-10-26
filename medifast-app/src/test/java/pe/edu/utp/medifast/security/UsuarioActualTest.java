package pe.edu.utp.medifast.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dao.PacienteDao;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Paciente;

@ExtendWith(MockitoExtension.class)
class UsuarioActualTest {

	@Mock
	private PacienteDao pacienteDao;

	@Mock
	private MedicoDao medicoDao;

	@InjectMocks
	private UsuarioActual usuarioActual;

	@Test
	void obtieneElIdPacienteDesdeElDocumentoAutenticado() {
		Authentication authentication = mock(Authentication.class);
		Paciente paciente = mock(Paciente.class);
		when(authentication.getName()).thenReturn("70000001");
		when(pacienteDao.findByUsuarioNumeroDocumento("70000001")).thenReturn(Optional.of(paciente));
		when(paciente.getId()).thenReturn(17L);

		assertThat(usuarioActual.idPaciente(authentication)).isEqualTo(17L);
	}

	@Test
	void obtieneElIdMedicoDesdeElDocumentoAutenticado() {
		Authentication authentication = mock(Authentication.class);
		Medico medico = mock(Medico.class);
		when(authentication.getName()).thenReturn("10000001");
		when(medicoDao.findByUsuarioNumeroDocumento("10000001")).thenReturn(Optional.of(medico));
		when(medico.getId()).thenReturn(3L);

		assertThat(usuarioActual.idMedico(authentication)).isEqualTo(3L);
	}

	@Test
	void reportaPerfilFaltanteParaUsuarioAutenticado() {
		Authentication authentication = mock(Authentication.class);
		when(authentication.getName()).thenReturn("70000001");
		when(pacienteDao.findByUsuarioNumeroDocumento("70000001")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> usuarioActual.idPaciente(authentication))
				.isInstanceOf(IllegalStateException.class);
	}
}
