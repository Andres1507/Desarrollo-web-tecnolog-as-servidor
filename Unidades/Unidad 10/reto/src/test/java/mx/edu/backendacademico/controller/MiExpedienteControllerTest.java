package mx.edu.backendacademico.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import mx.edu.backendacademico.config.SecurityConfig;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.service.AlumnoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest({MiExpedienteController.class, ExpedienteController.class})
@Import(SecurityConfig.class)
class MiExpedienteControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean AlumnoService service;

    @Test
    void alumnoConsultaSuPropioExpediente() throws Exception {
        when(service.buscarPorPropietario("alumno")).thenReturn(
            Optional.of(new Alumno(1L, "A1", "Ada", "ada@u.mx",
                EstatusAlumno.ACTIVO, "alumno"))
        );

        mvc.perform(get("/api/v1/mi-expediente")
                .with(httpBasic("alumno", "alumno123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.propietario").doesNotExist())
            .andExpect(jsonPath("$.matricula").value("A1"));
    }

    @Test
    void alumnoSinExpedienteRecibe404() throws Exception {
        when(service.buscarPorPropietario("alumno")).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/mi-expediente")
                .with(httpBasic("alumno", "alumno123")))
            .andExpect(status().isNotFound());
    }

    @Test
    void administradorPuedeConsultarExpedienteAjeno() throws Exception {
        when(service.buscar(1L)).thenReturn(
            new Alumno(1L, "A1", "Ada", "ada@u.mx",
                EstatusAlumno.ACTIVO, "alumno")
        );

        mvc.perform(get("/api/v1/expedientes/1")
                .with(httpBasic("admin", "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matricula").value("A1"));
    }

    @Test
    void usuarioNoAutenticadoNoPuedeConsultarSuExpediente() throws Exception {
        mvc.perform(get("/api/v1/mi-expediente"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void alumnoPropietarioPuedeConsultarExpedientePorId() throws Exception {
        when(service.buscar(1L)).thenReturn(
                new Alumno(1L, "A1", "Ada", "ada@u.mx",
                        EstatusAlumno.ACTIVO, "alumno")
        );

        mvc.perform(get("/api/v1/expedientes/1")
                        .with(httpBasic("alumno", "alumno123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value("A1"));
    }


    @Test
    void alumnoAjenoNoPuedeConsultarExpediente() throws Exception {
        when(service.buscar(1L)).thenReturn(
                new Alumno(1L, "A1", "Ada", "ada@u.mx",
                        EstatusAlumno.ACTIVO, "otraCuenta")
        );

        mvc.perform(get("/api/v1/expedientes/1")
                        .with(user("alumno").roles("ALUMNO")))
                .andExpect(status().isForbidden());
    }
}