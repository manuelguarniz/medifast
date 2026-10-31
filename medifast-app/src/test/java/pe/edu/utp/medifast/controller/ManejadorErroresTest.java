package pe.edu.utp.medifast.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.exception.HorarioNoDisponibleException;
import pe.edu.utp.medifast.exception.RecursoNoEncontradoException;

class ManejadorErroresTest {

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorPrueba()).setControllerAdvice(new ManejadorErrores()).build();
    }

    @Test
    void accesoDenegadoDevuelve403() throws Exception {
        mockMvc.perform(get("/prueba/403")).andExpect(status().isForbidden()).andExpect(view().name("error/403"));
    }

    @Test
    void recursoNoEncontradoDevuelve404() throws Exception {
        mockMvc.perform(get("/prueba/404")).andExpect(status().isNotFound()).andExpect(view().name("error/404"));
    }

    @Test
    void errorInesperadoDevuelve500() throws Exception {
        mockMvc.perform(get("/prueba/500")).andExpect(status().isInternalServerError()).andExpect(view().name("error/500"));
    }

    @Test
    void errorNegocioRedirigeConMensaje() throws Exception {
        mockMvc.perform(get("/prueba/negocio").header("Referer", "http://localhost/paciente/reservar")).andExpect(status().is3xxRedirection()).andExpect(view().name("redirect:/paciente/reservar")).andExpect(flash().attribute("error", "Horario no disponible, elija otro."));
    }

    @Test
    void noRedirigeADominioExterno() throws Exception {
        mockMvc.perform(get("/prueba/negocio").header("Referer", "https://sitio-externo.example/ruta")).andExpect(status().is3xxRedirection()).andExpect(view().name("redirect:/"));
    }

    @Controller
    static class ControladorPrueba {

        @GetMapping("/prueba/403")
        public String error403() {
            throw new AccesoDenegadoException("Acceso no autorizado.");
        }

        @GetMapping("/prueba/404")
        public String error404() {
            throw new RecursoNoEncontradoException("Recurso inexistente.");
        }

        @GetMapping("/prueba/500")
        public String error500() {
            throw new IllegalStateException("Error interno simulado.");
        }

        @GetMapping("/prueba/negocio")
        public String errorNegocio() {
            throw new HorarioNoDisponibleException();
        }
    }
}
