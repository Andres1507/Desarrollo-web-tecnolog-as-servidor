package mx.edu.backendacademico.controller;

import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.AlumnoResponse;
import mx.edu.backendacademico.service.AlumnoService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/expedientes")
public class ExpedienteController {

    private final AlumnoService service;

    public ExpedienteController(AlumnoService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public AlumnoResponse consultar(@PathVariable Long id,
                                    Authentication authentication) {

        Alumno alumno = service.buscar(id);

        boolean administrador = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean propietario = alumno.propietario()
                .equals(authentication.getName());

        if (!administrador && !propietario) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return AlumnoMapper.toResponse(alumno);
    }
}