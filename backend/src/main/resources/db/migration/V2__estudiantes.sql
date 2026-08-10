-- V2__estudiantes.sql
-- Flyway: catalogo de categorias y tabla de estudiantes del modulo academico.
-- Alineado con las entidades Categoria.java y Estudiante.java y con db/schema.sql.

-- Catalogo de categorias deportivas (deportivo.categorias)
CREATE TABLE IF NOT EXISTS deportivo.categorias (
    id_categoria BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    edad_min SMALLINT NOT NULL,
    edad_max SMALLINT NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Funcion generica para mantener updated_at en academico
CREATE OR REPLACE FUNCTION academico.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Tabla de estudiantes (academico.estudiantes)
CREATE TABLE IF NOT EXISTS academico.estudiantes (
    id_estudiante BIGSERIAL PRIMARY KEY,
    id_persona BIGINT NOT NULL REFERENCES seguridad.personas(id_persona),
    id_categoria BIGINT NOT NULL REFERENCES deportivo.categorias(id_categoria),
    id_estado_general BIGINT NOT NULL REFERENCES seguridad.estados_general(id_estado_general),
    codigo_estudiante VARCHAR(30) NOT NULL,
    fecha_ingreso DATE NOT NULL,
    peso NUMERIC(5,2),
    altura NUMERIC(5,2),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_estudiantes_codigo
    ON academico.estudiantes(codigo_estudiante);

CREATE TRIGGER trg_estudiantes_updated_at
BEFORE UPDATE ON academico.estudiantes
FOR EACH ROW EXECUTE FUNCTION academico.set_updated_at();
