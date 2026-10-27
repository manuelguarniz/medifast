package pe.edu.utp.medifast.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

class MedicoDaoTest {

	@Test
	void medicoActivoCargaElConsultorioAntesDeCerrarLaSesion() throws NoSuchMethodException {
		Method consulta = MedicoDao.class.getMethod("findFirstByActivoTrueOrderByIdAsc");

		assertThat(consulta.getAnnotation(EntityGraph.class).attributePaths())
				.containsExactly("consultorio");
	}
}
