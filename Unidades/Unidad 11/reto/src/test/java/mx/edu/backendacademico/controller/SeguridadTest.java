
package mx.edu.backendacademico.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mx.edu.backendacademico.config.SecurityConfig;
import mx.edu.backendacademico.domain.*;
import mx.edu.backendacademico.service.AlumnoService;
import mx.edu.backendacademico.service.AlumnoIdempotenciaService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlumnoController.class)
@Import(SecurityConfig.class)
class SeguridadTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AlumnoService service;

    @MockitoBean
    AlumnoIdempotenciaService idempotenciaService;

    private static final String DATOS = """
        {"matricula":"S1","nombre":"Ada","correo":"ada@u.mx"}
        """;

    @Test
    void sinCredencialesRecibe401() throws Exception {
        mvc.perform(get("/api/v1/alumnos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void alumnoNoPuedeCrearAunqueTengaCsrf() throws Exception {
        // Token correcto: el 403 se debe al rol.
        mvc.perform(post("/api/v1/alumnos")
                        .with(httpBasic("alumno", "alumno123"))
                        .with(csrf())
                        .header("Idempotency-Key", "seguridad-test-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS))
                .andExpect(status().isForbidden());
    }

    @Test
    void docenteSinCsrfRecibe403() throws Exception {
        // Rol correcto: falta el token CSRF.
        mvc.perform(post("/api/v1/alumnos")
                        .with(httpBasic("docente", "docente123"))
                        .header("Idempotency-Key", "seguridad-test-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS))
                .andExpect(status().isForbidden());
    }

    @Test
    void docenteConCsrfPuedeCrear() throws Exception {
        when(idempotenciaService.crear(
                anyString(),
                any()
        )).thenReturn(
                new AlumnoIdempotenciaService.Resultado(
                        201,
                        "{\"id\":1,\"matricula\":\"S1\",\"nombre\":\"Ada\",\"correo\":\"ada@u.mx\"}",
                        "/api/v1/alumnos/1"
                )
        );

        mvc.perform(post("/api/v1/alumnos")
                        .with(httpBasic("docente", "docente123"))
                        .with(csrf())
                        .header("Idempotency-Key", "seguridad-test-003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DATOS))
                .andExpect(status().isCreated());
    }
}
