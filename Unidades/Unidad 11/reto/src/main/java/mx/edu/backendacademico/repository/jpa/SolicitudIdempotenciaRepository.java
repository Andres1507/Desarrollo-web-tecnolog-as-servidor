
package mx.edu.backendacademico.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudIdempotenciaRepository
        extends JpaRepository<SolicitudIdempotenciaEntity, String> {

    @Modifying
    @Query(value = """
        INSERT INTO solicitudes_idempotencia
            (idempotency_key, request_hash)
        VALUES (:clave, :huella)
        ON CONFLICT (idempotency_key) DO NOTHING
        """, nativeQuery = true)
    int reservarClave(
            @Param("clave") String clave,
            @Param("huella") String huella);
}
