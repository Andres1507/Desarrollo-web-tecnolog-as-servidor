
package mx.edu.backendacademico.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import org.junit.jupiter.api.Test;

class AlumnoRepositoryConcurrenciaTest {

    @Test
    void dosTareasConLaMismaMatriculaProducenUnRegistroYUnConflicto()
            throws Exception {

        var repository = new AlumnoRepositoryEnMemoria();

        var preparadas = new CountDownLatch(2);
        var inicio = new CountDownLatch(1);
        var terminadas = new CountDownLatch(2);

        var registros = new AtomicInteger();
        var conflictos = new AtomicInteger();

        ExecutorService ejecutor = Executors.newFixedThreadPool(2);

        Runnable tarea = () -> {
            preparadas.countDown();

            try {
                inicio.await();

                repository.guardar(new Alumno(
                        null,
                        "A001",
                        "Alumno de prueba",
                        "prueba@uanl.mx",
                        EstatusAlumno.ACTIVO
                ));

                registros.incrementAndGet();

            } catch (MatriculaDuplicadaException e) {
                conflictos.incrementAndGet();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);

            } finally {
                terminadas.countDown();
            }
        };

        try {
            Future<?> tarea1 = ejecutor.submit(tarea);
            Future<?> tarea2 = ejecutor.submit(tarea);

            assertTrue(preparadas.await(5, TimeUnit.SECONDS));

            inicio.countDown();

            assertTrue(terminadas.await(5, TimeUnit.SECONDS));

            tarea1.get();
            tarea2.get();

            assertEquals(1, registros.get());
            assertEquals(1, conflictos.get());
            assertEquals(1, repository.buscarTodos().size());

        } finally {
            inicio.countDown();
            ejecutor.shutdownNow();
        }
    }
}
