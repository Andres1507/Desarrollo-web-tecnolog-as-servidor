
package mx.edu.backendacademico;

import java.util.concurrent.*;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.OperacionAcademicaException;
import mx.edu.backendacademico.repository.jpa.*;
import mx.edu.backendacademico.service.InscripcionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InscripcionConcurrenciaTest {

    @Autowired InscripcionService service;
    @Autowired AlumnoSpringDataRepository alumnos;
    @Autowired MateriaRepository materias;
    @Autowired InscripcionRepository inscripciones;

    @Test
    void dosAlumnosCompitenPorUnLugar() throws Exception {

        String sufijo = String.valueOf(System.nanoTime());

        var a1 = alumnos.saveAndFlush(new AlumnoEntity(
                new Alumno(null, "C1" + sufijo, "Alumno Uno",
                        "uno" + sufijo + "@test.mx", EstatusAlumno.ACTIVO)));

        var a2 = alumnos.saveAndFlush(new AlumnoEntity(
                new Alumno(null, "C2" + sufijo, "Alumno Dos",
                        "dos" + sufijo + "@test.mx", EstatusAlumno.ACTIVO)));

        var materia = materias.saveAndFlush(
                new MateriaEntity("C" + sufijo, "Concurrencia", 5, 1));

        var listos = new CountDownLatch(2);
        var inicio = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);

        try {
            Callable<String> tarea1 = () -> admitir(a1.getId(), materia.getId(), listos, inicio);
            Callable<String> tarea2 = () -> admitir(a2.getId(), materia.getId(), listos, inicio);

            Future<String> f1 = pool.submit(tarea1);
            Future<String> f2 = pool.submit(tarea2);

            assertTrue(listos.await(10, TimeUnit.SECONDS));
            inicio.countDown();

            String r1 = f1.get(20, TimeUnit.SECONDS);
            String r2 = f2.get(20, TimeUnit.SECONDS);

            System.out.println("TAREA 1: " + r1);
            System.out.println("TAREA 2: " + r2);

            assertNotEquals(r1.startsWith("ACEPTADA"),
                    r2.startsWith("ACEPTADA"));

            assertTrue(
                    (r1.equals("RECHAZADA:SIN_CUPO") ||
                            r2.equals("RECHAZADA:SIN_CUPO"))
            );

            assertEquals(1, inscripciones.countByMateriaId(materia.getId()));

            System.out.println("INSCRIPCIONES PERSISTIDAS: 1");

        } finally {
            pool.shutdownNow();

            var registros = inscripciones.findAll().stream()
                    .filter(i -> i.getMateria().getId().equals(materia.getId()))
                    .toList();

            inscripciones.deleteAll(registros);
            materias.delete(materia);
            alumnos.delete(a1);
            alumnos.delete(a2);
        }
    }

    private String admitir(Long alumnoId, Long materiaId,
                           CountDownLatch listos, CountDownLatch inicio)
            throws InterruptedException {

        listos.countDown();
        inicio.await();

        try {
            return "ACEPTADA: ID=" + service.inscribir(alumnoId, materiaId);
        } catch (OperacionAcademicaException ex) {
            return "RECHAZADA:" + ex.getCodigo();
        }
    }
}
