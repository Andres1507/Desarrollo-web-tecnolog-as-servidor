package mx.edu.backendacademico.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AlumnoTest {
    @Test void bajaConservaIdentidadSinModificarElOriginal() {
        var alumno = new Alumno(1L, " a001 ", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO);
        var baja = alumno.darDeBaja();
        // Comprueba tanto el resultado como la ausencia de efectos laterales.
        assertEquals("A001", baja.matricula());
        assertEquals(alumno.id(), baja.id());
        assertEquals(EstatusAlumno.BAJA, baja.estatus());
        assertEquals(EstatusAlumno.ACTIVO, alumno.estatus());
    }


    @Test
    void reactivarConservaElOriginal() {
        var alumno = new Alumno(1L, "A001", "Ada", "ada@u.mx", EstatusAlumno.BAJA);
        var activo = alumno.reactivar();

        assertEquals(EstatusAlumno.ACTIVO, activo.estatus());
        assertEquals(EstatusAlumno.BAJA, alumno.estatus());
        assertEquals(alumno.id(), activo.id());
        assertEquals(alumno.matricula(), activo.matricula());
    }


    @Test
    void mismoIdYDistintoCorreoNoSonIgualesPorValor() {
        var alumno1 = new Alumno(1L, "A001", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO);
        var alumno2 = new Alumno(1L, "A001", "Ada", "ada@otro.mx", EstatusAlumno.ACTIVO);

        assertNotEquals(alumno1, alumno2);
        assertEquals(alumno1.id(), alumno2.id());
    }


    @Test void rechazaMatriculaVacia() {
        assertThrows(IllegalArgumentException.class, () ->
                new Alumno(null, " ", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO));
    }

    @Test void materiaRechazaCreditosNoPositivos() {
        assertThrows(IllegalArgumentException.class, () -> new Materia(null, "M1", "Análisis", 0));
    }
}
