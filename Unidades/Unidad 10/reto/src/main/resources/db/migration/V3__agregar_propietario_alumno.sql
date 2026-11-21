ALTER TABLE alumnos ADD COLUMN propietario VARCHAR(80);
UPDATE alumnos SET propietario = 'alumno' WHERE propietario IS NULL;
ALTER TABLE alumnos ALTER COLUMN propietario SET NOT NULL;
