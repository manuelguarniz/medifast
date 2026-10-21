package pe.edu.utp.medifast.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import pe.edu.utp.medifast.dao.UsuarioDao;
import pe.edu.utp.medifast.model.Rol;
import pe.edu.utp.medifast.model.TipoDocumento;
import pe.edu.utp.medifast.model.Usuario;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

	@Mock
	private UsuarioDao usuarioDao;

	@InjectMocks
	private UsuarioDetailsService service;

	@Test
	void cargaUsuarioPorDocumentoConSuRol() {
		Usuario medico = new Usuario(TipoDocumento.DNI, "10000001", "$2a$10$hash", Rol.MEDICO);
		when(usuarioDao.findByNumeroDocumento("10000001")).thenReturn(Optional.of(medico));

		UserDetails detalles = service.loadUserByUsername("10000001");

		assertThat(detalles.getUsername()).isEqualTo("10000001");
		assertThat(detalles.getAuthorities()).extracting("authority").containsExactly("ROLE_MEDICO");
		assertThat(detalles.isEnabled()).isTrue();
	}

	@Test
	void usuarioInactivoQuedaDeshabilitado() {
		Usuario paciente = new Usuario(TipoDocumento.DNI, "70000001", "$2a$10$hash", Rol.PACIENTE);
		paciente.setActivo(false);
		when(usuarioDao.findByNumeroDocumento("70000001")).thenReturn(Optional.of(paciente));

		assertThat(service.loadUserByUsername("70000001").isEnabled()).isFalse();
	}

	@Test
	void documentoInexistenteLanzaExcepcion() {
		when(usuarioDao.findByNumeroDocumento("99999999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.loadUserByUsername("99999999"))
				.isInstanceOf(UsernameNotFoundException.class);
	}
}
