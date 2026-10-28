package pe.edu.utp.medifast.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.service.ConfirmacionPdfService;
import pe.edu.utp.medifast.util.EtiquetasEstado;
import pe.edu.utp.medifast.util.FormatoVista;

/** Arma la confirmación de RF15 en A5 a partir de {@code v_cita}; no incluye datos clínicos (RN-10). */
@Service
public class ConfirmacionPdfServiceImpl implements ConfirmacionPdfService {

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy",
			FormatoVista.LOCALE);
	private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
			FormatoVista.LOCALE);
	private static final Color VERDE = new Color(0x0d, 0x5c, 0x4d);
	private static final Color GRIS = new Color(0x64, 0x74, 0x8b);
	private static final Color BORDE = new Color(0xe2, 0xe8, 0xf0);

	private final VistaCitaDao vistaCitaDao;
	private final MedicoDao medicoDao;
	private final Clock clock;

	public ConfirmacionPdfServiceImpl(VistaCitaDao vistaCitaDao, MedicoDao medicoDao, Clock clock) {
		this.vistaCitaDao = vistaCitaDao;
		this.medicoDao = medicoDao;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public byte[] generar(Long idCita, Long idPaciente) {
		VistaCita cita = vistaCitaDao.findById(idCita)
				.filter(encontrada -> encontrada.getIdPaciente().equals(idPaciente))
				.orElseThrow(() -> new AccesoDenegadoException("La cita no pertenece al paciente autenticado."));
		if (cita.getEstado() != EstadoCitaEnum.PROGRAMADA && cita.getEstado() != EstadoCitaEnum.ATENDIDA) {
			throw new AccesoDenegadoException("La confirmación solo está disponible para citas confirmadas o atendidas.");
		}
		String especialidad = medicoDao.findById(cita.getIdMedico()).map(Medico::getEspecialidad).orElse("");

		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		Document documento = new Document(PageSize.A5, 36, 36, 40, 36);
		PdfWriter.getInstance(documento, salida);
		String numeroCita = FormatoVista.numeroCita(cita.getIdCita());
		documento.addTitle("Confirmación de cita " + numeroCita);
		documento.open();

		documento.add(new Paragraph("MEDIFAST", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.NORMAL, VERDE)));
		documento.add(new Paragraph("Consultorio Medifast", fuente(10, GRIS)));
		Paragraph titulo = new Paragraph("Confirmación de cita",
				FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Font.NORMAL, Color.BLACK));
		titulo.setSpacingBefore(18);
		documento.add(titulo);
		Paragraph numero = new Paragraph("N.º " + numeroCita, fuente(11, GRIS));
		numero.setSpacingAfter(12);
		documento.add(numero);

		PdfPTable tabla = new PdfPTable(new float[] { 2, 3 });
		tabla.setWidthPercentage(100);
		fila(tabla, "Paciente", cita.getPaciente());
		fila(tabla, "Médico", FormatoVista.doctor(cita.getMedico()));
		if (!especialidad.isBlank()) {
			fila(tabla, "Especialidad", especialidad);
		}
		fila(tabla, "Fecha", FormatoVista.capitalizar(cita.getFecha().format(FECHA)));
		fila(tabla, "Hora", FormatoVista.rangoHoras12Espaciado(cita.getHoraInicio(), cita.getHoraFin()));
		fila(tabla, "Tipo", EtiquetasEstado.tipo(cita.getTipoCita()));
		fila(tabla, "Monto", FormatoVista.solesOGuion(cita.getMonto()));
		fila(tabla, "Estado de la cita", EtiquetasEstado.cita(cita.getEstado()));
		fila(tabla, "Estado del pago", EtiquetasEstado.pago(cita.getEstadoPago()));
		documento.add(tabla);

		Paragraph nota = new Paragraph("Te recomendamos llegar entre 15 y 30 minutos antes de tu cita.", fuente(9, GRIS));
		nota.setSpacingBefore(16);
		documento.add(nota);
		Paragraph pie = new Paragraph("Generado el " + LocalDateTime.now(clock).format(GENERADO)
				+ " · Pago simulado con Yape, no se realizó ningún cobro.", fuente(8, GRIS));
		pie.setAlignment(Element.ALIGN_CENTER);
		pie.setSpacingBefore(24);
		documento.add(pie);
		documento.close();
		return salida.toByteArray();
	}

	private static void fila(PdfPTable tabla, String etiqueta, String valor) {
		tabla.addCell(celda(etiqueta, fuente(10, GRIS)));
		tabla.addCell(celda(valor, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.NORMAL, Color.BLACK)));
	}

	private static PdfPCell celda(String texto, Font fuente) {
		PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
		celda.setBorder(Rectangle.BOTTOM);
		celda.setBorderColor(BORDE);
		celda.setPadding(7);
		return celda;
	}

	private static Font fuente(float tamano, Color color) {
		return FontFactory.getFont(FontFactory.HELVETICA, tamano, Font.NORMAL, color);
	}
}
