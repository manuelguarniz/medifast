package pe.edu.utp.medifast.service;

import java.time.LocalDate;

import pe.edu.utp.medifast.dto.CitaPacienteDto;
import pe.edu.utp.medifast.dto.DisponibilidadDto;
import pe.edu.utp.medifast.dto.MisCitasDto;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;

/** Reglas de negocio para consultar horarios y reservar citas (RF01, RF05). */
public interface ReservaService {

	/**
	 * Devuelve la tarifa, la ventana de reserva, los inicios con suficientes bloques consecutivos y la cita activa,
	 * si existe.
	 *
	 * @param idPaciente paciente que consulta
	 * @param idMedico médico cuya agenda se consulta
	 * @param fecha día solicitado
	 * @return disponibilidad calculada según el tipo de cita y la configuración del consultorio
	 * @throws pe.edu.utp.medifast.exception.FueraDeRangoException si la fecha no está dentro de la ventana permitida
	 */
	DisponibilidadDto disponibilidad(Long idPaciente, Long idMedico, LocalDate fecha);

	/**
	 * Detecta si el paciente es nuevo o de control con el médico indicado (RN-02).
	 *
	 * @param idPaciente paciente
	 * @param idMedico médico
	 * @return tipo de cita
	 */
	TipoCitaEnum detectarTipo(Long idPaciente, Long idMedico);

	/**
	 * Reserva el inicio elegido y todos los bloques consecutivos que requiere la tarifa (RF01).
	 *
	 * @param idPaciente paciente que reserva
	 * @param idHorario id del primer bloque
	 * @return cita creada en estado PENDIENTE_PAGO
	 * @throws pe.edu.utp.medifast.exception.CitaActivaException si el paciente ya tiene una cita activa
	 * @throws pe.edu.utp.medifast.exception.FueraDeRangoException si el horario está fuera de la ventana permitida
	 * @throws pe.edu.utp.medifast.exception.HorarioNoDisponibleException si el bloque no está libre o la BD detecta una colisión
	 */
	Cita reservar(Long idPaciente, Long idHorario);

	/**
	 * Citas del paciente para "Mis citas" (P5, RF10): separa la cita activa del resto y marca como vencidas las
	 * {@code PENDIENTE_PAGO} cuyo plazo pasó y que RF16 aún no canceló. Si la cita no tiene pago, el monto sale de
	 * la tarifa de su tipo (RN-03). Del historial lee de la BD solo la página pedida; una página inválida o fuera de
	 * rango se ajusta a la más cercana.
	 *
	 * @param idPaciente paciente autenticado
	 * @param pagina número de página del historial (desde 1), tal como llega en la URL
	 * @return cita activa, si existe, y la página del historial de la más reciente a la más antigua
	 * @throws jakarta.persistence.EntityNotFoundException si no existe el paciente o la tarifa de una cita sin pago
	 */
	MisCitasDto historial(Long idPaciente, String pagina);

	/**
	 * Una cita del paciente para la confirmación o el detalle (P4, RF15), con el monto y el plazo calculados igual
	 * que en {@link #historial(Long, String)}.
	 *
	 * @param idCita cita solicitada
	 * @param idPaciente paciente autenticado
	 * @return cita con su monto, si venció y los segundos que le quedan para pagar
	 * @throws pe.edu.utp.medifast.exception.AccesoDenegadoException si la cita no existe o es de otro paciente
	 */
	CitaPacienteDto cita(Long idCita, Long idPaciente);
}
