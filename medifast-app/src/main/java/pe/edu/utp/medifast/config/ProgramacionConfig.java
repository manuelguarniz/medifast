package pe.edu.utp.medifast.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas usando el reloj oficial del consultorio. */
@Configuration
@EnableScheduling
public class ProgramacionConfig {

	@Bean
	Clock relojMedifast() {
		return Clock.system(ZoneId.of("America/Lima"));
	}
}
