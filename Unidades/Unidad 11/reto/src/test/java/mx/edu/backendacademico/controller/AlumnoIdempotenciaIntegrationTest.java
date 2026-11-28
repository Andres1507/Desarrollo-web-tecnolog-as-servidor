
package mx.edu.backendacademico.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AlumnoIdempotenciaIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void configurarBaseDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void dosSolicitudesSimultaneasConLaMismaClaveCreanUnSoloAlumno()
            throws Exception {

        String clave = "prueba-concurrente-001";
        String matricula = "IDEMP001";

        String contenido = """
            {
                "matricula": "%s",
                "nombre": "Alumno Idempotente",
                "correo": "idempotente@test.com"
            }
            """.formatted(matricula);

        CountDownLatch preparadas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

            Future<MvcResult> primera = executor.submit(() -> {
                preparadas.countDown();
                iniciar.await();
                return enviarSolicitud(clave, contenido);
            });

            Future<MvcResult> segunda = executor.submit(() -> {
                preparadas.countDown();
                iniciar.await();
                return enviarSolicitud(clave, contenido);
            });

            assertThat(preparadas.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();

            MvcResult resultado1 = primera.get(30, TimeUnit.SECONDS);
            MvcResult resultado2 = segunda.get(30, TimeUnit.SECONDS);

            assertThat(resultado1.getResponse().getStatus()).isEqualTo(201);
            assertThat(resultado2.getResponse().getStatus()).isEqualTo(201);

            assertThat(resultado1.getResponse().getContentAsString())
                    .isEqualTo(resultado2.getResponse().getContentAsString());

            Integer cantidad = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM alumnos WHERE matricula = ?",
                    Integer.class,
                    matricula);

            assertThat(cantidad).isEqualTo(1);
        }
    }

    @Test
    void mismaClaveConContenidoDiferenteDevuelveConflicto()
            throws Exception {

        String clave = "prueba-contenido-001";

        String contenido1 = """
            {
                "matricula": "IDEMP002",
                "nombre": "Primer Alumno",
                "correo": "primero@test.com"
            }
            """;

        String contenido2 = """
            {
                "matricula": "IDEMP003",
                "nombre": "Segundo Alumno",
                "correo": "segundo@test.com"
            }
            """;

        MvcResult primera = enviarSolicitud(clave, contenido1);
        MvcResult segunda = enviarSolicitud(clave, contenido2);

        assertThat(primera.getResponse().getStatus()).isEqualTo(201);
        assertThat(segunda.getResponse().getStatus()).isEqualTo(409);

        Integer cantidad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alumnos WHERE matricula IN (?, ?)",
                Integer.class,
                "IDEMP002",
                "IDEMP003");

        assertThat(cantidad).isEqualTo(1);
    }

    private MvcResult enviarSolicitud(String clave, String contenido)
            throws Exception {

        return mockMvc.perform(post("/api/v1/alumnos")
                        .with(httpBasic("docente", "docente123"))
                        .with(csrf())
                        .header("Idempotency-Key", clave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contenido))
                .andReturn();
    }
}
