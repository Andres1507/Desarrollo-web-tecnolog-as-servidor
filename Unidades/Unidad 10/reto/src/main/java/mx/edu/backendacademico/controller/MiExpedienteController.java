package mx.edu.backendacademico.controller;

import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.AlumnoResponse;
import mx.edu.backendacademico.service.AlumnoService;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MiExpedienteController {

    private final AlumnoService service;

    public MiExpedienteController(AlumnoService service) {
        this.service = service;
    }

    @GetMapping("/mi-expediente")
    public ResponseEntity<AlumnoResponse> consultar(Principal principal) {
        return service.buscarPorPropietario(principal.getName())
                .map(AlumnoMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}