-- V5__procedimientos_almacenados.sql
-- Funciones de agregados/operaciones masivas sobre academico.estudiantes.
-- (V6 las convierte en PROCEDIMIENTOS reales: Spring Data JPA los invoca
--  con {call ...}, que PostgreSQL solo acepta contra CREATE PROCEDURE.)
-- Alineado con academico.estudiantes y su FK id_categoria.

-- fn_contar_estudiantes_activos
-- Propósito: contar estudiantes activos de una categoría (agregado COUNT,
--            obligatoriamente en el motor según Bloque A.2.2).
-- Entrada:  p_categoria INT (id_categoria)
-- Salida:   BIGINT (total de estudiantes activos de esa categoría)
-- Sin SQL dinámico. Parámetros nombrados.
CREATE OR REPLACE FUNCTION academico.fn_contar_estudiantes_activos(
    p_categoria INT
)
RETURNS BIGINT
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_total BIGINT;
BEGIN
    SELECT COUNT(*)
      INTO v_total
      FROM academico.estudiantes e
     WHERE e.activo = TRUE
       AND e.id_categoria = p_categoria;
    RETURN v_total;
END;
$$;

-- fn_desactivar_estudiantes_categoria
-- Propósito: baja lógica masiva de todos los estudiantes activos de una
--            categoría (actualización masiva con criterio de negocio,
--            obligatoriamente en el motor según Bloque A.2.2).
-- Entrada:  p_categoria INT (id_categoria)
-- Salida:   INTEGER (número de filas afectadas)
-- Sin SQL dinámico. Parámetros nombrados.
CREATE OR REPLACE FUNCTION academico.fn_desactivar_estudiantes_categoria(
    p_categoria INT
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_afectados INTEGER;
BEGIN
    UPDATE academico.estudiantes e
       SET activo = FALSE
     WHERE e.activo = TRUE
       AND e.id_categoria = p_categoria;

    GET DIAGNOSTICS v_afectados = ROW_COUNT;
    RETURN v_afectados;
END;
$$;
