package pe.edu.utp.medifast.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dto.ConstanciaDto;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.exception.RecursoNoEncontradoException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.Pago;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConstanciaServiceImplTest {

	private static final long ID_CITA = 124L;
	private static final long ID_PACIENTE = 20L;
	private static final long ID_MEDICO = 1L;

	@TempDir
	private Path carpeta;
	@Mock
	private CitaDao citaDao;
	@Mock
	private PagoDao pagoDao;
	@Mock
	private Cita cita;
	@Mock
	private Pago pago;

	private ConstanciaServiceImpl servicio;

	@BeforeEach
	void configurar() {
		servicio = new ConstanciaServiceImpl(citaDao, pagoDao, carpeta.toString());
		Paciente paciente = mock(Paciente.class);
		when(paciente.getId()).thenReturn(ID_PACIENTE);
		when(cita.getId()).thenReturn(ID_CITA);
		when(cita.getPaciente()).thenReturn(paciente);
		Medico medico = mock(Medico.class);
		when(medico.getId()).thenReturn(ID_MEDICO);
		when(cita.getMedico()).thenReturn(medico);
		when(citaDao.findById(ID_CITA)).thenReturn(Optional.of(cita));
		when(pago.getComprobanteTipo()).thenReturn("image/png");
	}

	@Test
	void devuelveLaConstanciaAlPacienteDuenoConUnNombreSinUuid() throws Exception {
		Path archivo = Files.writeString(carpeta.resolve("0b7c2e1a-uuid.png"), "png");
		when(pago.getComprobanteRuta()).thenReturn(archivo.toString());
		when(pagoDao.findByCitaId(ID_CITA)).thenReturn(Optional.of(pago));

		ConstanciaDto constancia = servicio.paraPaciente(ID_CITA, ID_PACIENTE);

		assertThat(constancia.tipo()).isEqualTo("image/png");
		assertThat(constancia.nombre()).isEqualTo("constancia-000124.png");
		assertThat(constancia.archivo().getContentAsByteArray()).isEqualTo("png".getBytes());
	}

	@Test
	void niegaLaConstanciaAOtroPaciente() {
		assertThatThrownBy(() -> servicio.paraPaciente(ID_CITA, 99L)).isInstanceOf(AccesoDenegadoException.class);
	}

	@Test
	void devuelveLaConstanciaAlMedicoDeLaCita() throws Exception {
		Path archivo = Files.writeString(carpeta.resolve("0b7c2e1a-uuid.png"), "png");
		when(pago.getComprobanteRuta()).thenReturn(archivo.toString());
		when(pagoDao.findByCitaId(ID_CITA)).thenReturn(Optional.of(pago));

		ConstanciaDto constancia = servicio.paraMedico(ID_CITA, ID_MEDICO);

		assertThat(constancia.nombre()).isEqualTo("constancia-000124.png");
		assertThat(constancia.archivo().getContentAsByteArray()).isEqualTo("png".getBytes());
	}

	@Test
	void niegaLaConstanciaAOtroMedico() {
		assertThatThrownBy(() -> servicio.paraMedico(ID_CITA, 2L)).isInstanceOf(AccesoDenegadoException.class);
	}

	@Test
	void niegaUnaCitaInexistenteSinRevelarQueNoExiste() {
		when(citaDao.findById(500L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.paraPaciente(500L, ID_PACIENTE)).isInstanceOf(AccesoDenegadoException.class);
	}

	@Test
	void respondeNoEncontradoSiLaCitaNoTienePago() {
		when(pagoDao.findByCitaId(ID_CITA)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.paraPaciente(ID_CITA, ID_PACIENTE))
				.isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void noSirveArchivosFueraDeLaCarpetaDeConstancias(@TempDir Path otraCarpeta) throws Exception {
		Path ajeno = Files.writeString(otraCarpeta.resolve("secreto.png"), "x");
		when(pago.getComprobanteRuta()).thenReturn(carpeta.resolve("../" + otraCarpeta.getFileName())
				.resolve(ajeno.getFileName()).toString());
		when(pagoDao.findByCitaId(ID_CITA)).thenReturn(Optional.of(pago));

		assertThatThrownBy(() -> servicio.paraPaciente(ID_CITA, ID_PACIENTE))
				.isInstanceOf(RecursoNoEncontradoException.class);
	}
}
