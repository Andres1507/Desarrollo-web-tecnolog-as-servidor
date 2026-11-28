
package mx.edu.backendacademico.repository.jpa;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "solicitudes_idempotencia")
public class SolicitudIdempotenciaEntity {

    @Id
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_location", length = 512)
    private String responseLocation;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected SolicitudIdempotenciaEntity() {
    }

    public SolicitudIdempotenciaEntity(String idempotencyKey, String requestHash) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
    }

    public void completarRespuesta(
            Integer responseStatus,
            String responseBody,
            String responseLocation) {

        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.responseLocation = responseLocation;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getResponseLocation() {
        return responseLocation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
