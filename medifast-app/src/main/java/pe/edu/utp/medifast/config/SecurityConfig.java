package pe.edu.utp.medifast.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;
import pe.edu.utp.medifast.security.RedireccionPorRol;

/**
 * Seguridad: login por n.º de documento, acceso por rol, bcrypt y CSRF (activo por defecto).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final RedireccionPorRol redireccionPorRol;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/login", "/registro", "/error", "/css/**", "/js/**", "/img/**").permitAll()
						.requestMatchers("/paciente/**").hasRole("PACIENTE")
						.requestMatchers("/medico/**").hasRole("MEDICO")
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.usernameParameter("numeroDocumento")
						.passwordParameter("contrasena")
						.successHandler(redireccionPorRol)
						.failureHandler((request, response, exception) -> response.sendRedirect(
								request.getContextPath()
										+ (exception instanceof LockedException ? "/login?bloqueado" : "/login?error")))
						.permitAll())
				.logout(logout -> logout
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID")
						.logoutSuccessUrl("/login?salir")
						.permitAll())
				.sessionManagement(session -> session.invalidSessionUrl("/login?expirada"));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(10);
	}
}
