package pe.edu.utp.medifast.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import pe.edu.utp.medifast.dao.UsuarioDao;

/**
 * Carga el usuario para Spring Security usando el n.º de documento como login (RF07, RN-12).
 */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioDao usuarioDao;

	@Override
	public UserDetails loadUserByUsername(String numeroDocumento) {
		return usuarioDao.findByNumeroDocumento(numeroDocumento)
				.map(usuario -> User.withUsername(usuario.getNumeroDocumento())
						.password(usuario.getContrasenaHash())
						.roles(usuario.getRol().name())
						.disabled(!usuario.isActivo())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
	}
}
