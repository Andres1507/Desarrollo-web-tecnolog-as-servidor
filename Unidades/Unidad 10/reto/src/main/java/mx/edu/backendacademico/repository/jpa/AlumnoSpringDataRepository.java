package mx.edu.backendacademico.repository.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlumnoSpringDataRepository extends JpaRepository<AlumnoEntity, Long> {
    boolean existsByMatriculaIgnoreCase(String matricula);

    Optional<AlumnoEntity> findByPropietario(String propietario);
}