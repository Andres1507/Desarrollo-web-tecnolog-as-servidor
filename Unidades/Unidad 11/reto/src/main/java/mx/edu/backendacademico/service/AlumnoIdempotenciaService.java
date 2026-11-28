
package mx.edu.backendacademico.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.CrearAlumnoRequest;
import mx.edu.backendacademico.repository.jpa.SolicitudIdempotenciaEntity;
import mx.edu.backendacademico.repository.jpa.SolicitudIdempotenciaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlumnoIdempotenciaService {

    private final SolicitudIdempotenciaRepository repository;
    private final AlumnoService alumnoService;
    private final ObjectMapper objectMapper;

    public AlumnoIdempotenciaService(
            SolicitudIdempotenciaRepository repository,
            AlumnoService alumnoService,
            ObjectMapper objectMapper) {

        this.repository = repository;
        this.alumnoService = alumnoService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Resultado crear(String clave, CrearAlumnoRequest request)
            throws JacksonException {

        String huella = calcularHuella(request);

        int reservada = repository.reservarClave(clave, huella);

        if (reservada == 0) {

            SolicitudIdempotenciaEntity existente =
                    repository.findById(clave)
                            .orElseThrow(() -> new IllegalStateException(
                                    "No se encontró la clave de idempotencia."));

            if (!existente.getRequestHash().equals(huella)) {
                return new Resultado(
                        409,
                        "{\"error\":\"La clave ya fue utilizada con otro contenido.\"}",
                        null);
            }

            return new Resultado(
                    existente.getResponseStatus(),
                    existente.getResponseBody(),
                    existente.getResponseLocation());
        }

        var alumno = alumnoService.crear(
                request.matricula(),
                request.nombre(),
                request.correo());

        var respuesta = AlumnoMapper.toResponse(alumno);

        String cuerpo = objectMapper.writeValueAsString(respuesta);
        String ubicacion = "/api/v1/alumnos/" + alumno.id();

        SolicitudIdempotenciaEntity registro =
                repository.findById(clave)
                        .orElseThrow(() -> new IllegalStateException(
                                "No se encontró la clave recién registrada."));

        registro.completarRespuesta(201, cuerpo, ubicacion);
        repository.save(registro);

        return new Resultado(201, cuerpo, ubicacion);
    }

    private String calcularHuella(CrearAlumnoRequest request)
            throws JacksonException {

        byte[] contenido = objectMapper.writeValueAsBytes(request);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contenido));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "No fue posible calcular la huella SHA-256.", ex);
        }
    }

    public record Resultado(
            int status,
            String body,
            String location
    ) {
    }
}
