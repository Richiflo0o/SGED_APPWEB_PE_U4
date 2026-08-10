-- V1__schema_inicial.sql
-- Flyway: esquemas base y tablas del modulo seguridad.
-- Alineado con las entidades JPA (ddl-auto=validate) y con db/schema.sql.

-- Esquemas
CREATE SCHEMA IF NOT EXISTS seguridad;
CREATE SCHEMA IF NOT EXISTS deportivo;
CREATE SCHEMA IF NOT EXISTS academico;

-- Tabla de estados general
CREATE TABLE IF NOT EXISTS seguridad.estados_general (
    id_estado_general BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

-- Tabla de personas (alineada con Persona.java)
CREATE TABLE IF NOT EXISTS seguridad.personas (
    id_persona BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    cedula VARCHAR(10) NOT NULL,
    correo VARCHAR(200) NOT NULL,
    telefono VARCHAR(15),
    foto TEXT,
    fecha_nacimiento DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_personas_cedula ON seguridad.personas(cedula);
CREATE UNIQUE INDEX IF NOT EXISTS idx_personas_correo ON seguridad.personas(correo);

-- Tabla de roles
CREATE TABLE IF NOT EXISTS seguridad.roles (
    id_rol BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255)
);

-- Tabla de usuarios (alineada con Usuario.java)
CREATE TABLE IF NOT EXISTS seguridad.usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    id_persona BIGINT NOT NULL REFERENCES seguridad.personas(id_persona),
    id_estado_general BIGINT NOT NULL REFERENCES seguridad.estados_general(id_estado_general),
    username VARCHAR(50) NOT NULL,
    password_hash TEXT NOT NULL,
    ultimo_acceso TIMESTAMPTZ,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuarios_username ON seguridad.usuarios(username);

-- Tabla de usuario_rol (relacion N:N mapeada con @JoinTable)
CREATE TABLE IF NOT EXISTS seguridad.usuario_rol (
    id_usuario_rol BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES seguridad.usuarios(id_usuario),
    id_rol BIGINT NOT NULL REFERENCES seguridad.roles(id_rol)
);
