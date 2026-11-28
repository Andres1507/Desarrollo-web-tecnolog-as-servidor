
CREATE TABLE solicitudes_idempotencia (
                                          idempotency_key VARCHAR(128) NOT NULL,
                                          request_hash CHAR(64) NOT NULL,
                                          response_status INTEGER,
                                          response_body TEXT,
                                          response_location VARCHAR(512),
                                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                          CONSTRAINT pk_solicitudes_idempotencia
                                              PRIMARY KEY (idempotency_key)
);
