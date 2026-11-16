package pe.edu.utp.medifast.controller;

import java.net.URI;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.exception.AccionNoPermitidaException;
import pe.edu.utp.medifast.exception.CitaActivaException;
import pe.edu.utp.medifast.exception.ContrasenaIncorrectaException;
import pe.edu.utp.medifast.exception.CorreoDuplicadoException;
import pe.edu.utp.medifast.exception.DocumentoDuplicadoException;
import pe.edu.utp.medifast.exception.FueraDeRangoException;
import pe.edu.utp.medifast.exception.HorarioNoDisponibleException;
import pe.edu.utp.medifast.exception.PagoInvalidoException;
import pe.edu.utp.medifast.exception.PagoNoValidadoException;
import pe.edu.utp.medifast.exception.RecursoNoEncontradoException;
import pe.edu.utp.medifast.exception.ReservaVencidaException;

@ControllerAdvice(annotations = Controller.class)
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    private static final String ERROR_403 = "error/403";
    private static final String ERROR_404 = "error/404";
    private static final String ERROR_500 = "error/500";

    private static final Set<String> ESQUEMAS = Set.of("http", "https");

    @ExceptionHandler(AccesoDenegadoException.class)
    public ModelAndView accesoDenegado(AccesoDenegadoException ex, HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return new ModelAndView(ERROR_403);
    }

    @ExceptionHandler({NoResourceFoundException.class, RecursoNoEncontradoException.class})
    public ModelAndView noEncontrado(Exception ex, HttpServletResponse response) {

        response.setStatus(HttpStatus.NOT_FOUND.value());
        return new ModelAndView(ERROR_404);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoGrande(MaxUploadSizeExceededException ex, HttpServletRequest request, RedirectAttributes redirect) {

        redirect.addFlashAttribute("error", "El archivo supera 5 MB"
        );

        return regresar(request);
    }

    @ExceptionHandler({
        AccionNoPermitidaException.class,
        CitaActivaException.class,
        ContrasenaIncorrectaException.class,
        CorreoDuplicadoException.class,
        DocumentoDuplicadoException.class,
        FueraDeRangoException.class,
        HorarioNoDisponibleException.class,
        PagoInvalidoException.class,
        PagoNoValidadoException.class,
        ReservaVencidaException.class
    })
    public String errorNegocio(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirect) {
        String mensaje = ex.getMessage();

        if (mensaje == null || mensaje.isBlank()) {
            mensaje = "No se pudo completar la operación.";
        }

        redirect.addFlashAttribute("error", mensaje);

        return regresar(request);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView errorInterno(Exception ex, HttpServletRequest request, HttpServletResponse response) {

        log.error("Error interno MEDIFAST. Tipo: {}, ruta: {}", ex.getClass().getSimpleName(), request.getRequestURI());

        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ModelAndView(ERROR_500);
    }

    private String regresar(HttpServletRequest request) {

        String referer = request.getHeader("Referer");

        if (referer == null || referer.isBlank()) {
            return "redirect:/";
        }

        try {
            URI anterior = URI.create(referer);
            String esquema = anterior.getScheme();
            String servidor = anterior.getHost();

            if (esquema == null || servidor == null || !ESQUEMAS.contains(esquema.toLowerCase())) {
                return "redirect:/";
            }

            boolean mismoServidor = servidor.equalsIgnoreCase(request.getServerName());

            int puertoAnterior = anterior.getPort();

            int puertoActual = request.getServerPort();

            boolean mismoPuerto = puertoAnterior == puertoActual || (puertoAnterior == -1 && (("https".equalsIgnoreCase(esquema) && puertoActual == 443) || ("http".equalsIgnoreCase(esquema) && puertoActual == 80)));

            if (!mismoServidor || !mismoPuerto || !esquema.equalsIgnoreCase(request.getScheme())) {
                return "redirect:/";
            }

            String ruta = anterior.getRawPath();

            if (ruta == null || ruta.isBlank() || ruta.equals(request.getRequestURI())) {
                return "redirect:/";
            }

            String contexto = request.getContextPath();

            if (!contexto.isEmpty()) {
                if (!ruta.equals(contexto) && !ruta.startsWith(contexto + "/")) {
                    return "redirect:/";
                }
                ruta = ruta.substring(contexto.length());
            }

            String consulta = anterior.getRawQuery();

            return "redirect:" + ruta + (consulta == null ? "" : "?" + consulta);

        } catch (IllegalArgumentException ex) {
            return "redirect:/";
        }
    }
}
