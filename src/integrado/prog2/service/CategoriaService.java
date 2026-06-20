package integrado.prog2.service;

import integrado.prog2.config.ConexionDB;
import integrado.prog2.entities.Categoria;
import integrado.prog2.entities.Producto;
import integrado.prog2.exception.EntidadNoEncontradaException;
import integrado.prog2.exception.ReglaNegocioException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaService {

    public List<Categoria> listar() {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT * FROM categorias WHERE eliminado = 0";
        try (Connection conn = ConexionDB.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(mapResultSetToCategoria(rs));
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al listar categorías desde la base de datos: " + e.getMessage());
        }
        return lista;
    }

    public Categoria buscarPorId(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de la categoría no puede ser nulo.");
        }
        String sql = "SELECT * FROM categorias WHERE id = ? AND eliminado = 0";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCategoria(rs);
                } else {
                    throw new EntidadNoEncontradaException("Categoría no encontrada con ID: " + id);
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al buscar categoría por ID: " + e.getMessage());
        }
    }

    public Categoria crear(String nombre, String descripcion) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre de la categoría no puede estar vacío.");
        }

        String checkSql = "SELECT COUNT(*) FROM categorias WHERE nombre = ? AND eliminado = 0";
        String insertSql = "INSERT INTO categorias (nombre, descripcion, eliminado) VALUES (?, ?, 0)";

        try (Connection conn = ConexionDB.obtenerConexion()) {
            // Verificar nombre único
            try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                checkPstmt.setString(1, nombre.trim());
                try (ResultSet rs = checkPstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new ReglaNegocioException("Ya existe una categoría con el nombre: " + nombre);
                    }
                }
            }

            // Insertar categoría
            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPstmt.setString(1, nombre.trim());
                insertPstmt.setString(2, descripcion != null ? descripcion.trim() : "");
                insertPstmt.executeUpdate();

                try (ResultSet generatedKeys = insertPstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Long generatedId = generatedKeys.getLong(1);
                        return buscarPorId(generatedId);
                    } else {
                        throw new ReglaNegocioException("Error al obtener el ID generado de la categoría.");
                    }
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al crear categoría en la base de datos: " + e.getMessage());
        }
    }

    public Categoria editar(Long id, String nombre, String descripcion) {
        Categoria cat = buscarPorId(id); // Valida que exista y no esté eliminada

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre de la categoría no puede estar vacío.");
        }

        String checkSql = "SELECT COUNT(*) FROM categorias WHERE nombre = ? AND id != ? AND eliminado = 0";
        String updateSql = "UPDATE categorias SET nombre = ?, descripcion = ? WHERE id = ? AND eliminado = 0";

        try (Connection conn = ConexionDB.obtenerConexion()) {
            // Verificar unicidad de nombre contra otras categorías
            try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                checkPstmt.setString(1, nombre.trim());
                checkPstmt.setLong(2, id);
                try (ResultSet rs = checkPstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new ReglaNegocioException("Ya existe otra categoría con el nombre: " + nombre);
                    }
                }
            }

            // Actualizar categoría
            try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                updatePstmt.setString(1, nombre.trim());
                updatePstmt.setString(2, descripcion != null ? descripcion.trim() : "");
                updatePstmt.setLong(3, id);
                updatePstmt.executeUpdate();
                
                return buscarPorId(id);
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al editar categoría en la base de datos: " + e.getMessage());
        }
    }

    /**
     * Elimina lógicamente una categoría.
     * En lugar de tomar una lista de productos en memoria, realiza la consulta de productos asociados directamente en SQL.
     */
    public void eliminar(Long id, List<Producto> productosActivosUnused) {
        Categoria cat = buscarPorId(id); // Valida que exista

        String countProductsSql = "SELECT COUNT(*) FROM productos WHERE categoria_id = ? AND eliminado = 0";
        String deleteSql = "UPDATE categorias SET eliminado = 1 WHERE id = ?";

        try (Connection conn = ConexionDB.obtenerConexion()) {
            // Validar que no tenga productos activos asociados
            try (PreparedStatement countPstmt = conn.prepareStatement(countProductsSql)) {
                countPstmt.setLong(1, id);
                try (ResultSet rs = countPstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new ReglaNegocioException("No se puede eliminar la categoría porque tiene productos activos asociados.");
                    }
                }
            }

            // Realizar la baja lógica
            try (PreparedStatement deletePstmt = conn.prepareStatement(deleteSql)) {
                deletePstmt.setLong(1, id);
                deletePstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al eliminar categoría en la base de datos: " + e.getMessage());
        }
    }

    private Categoria mapResultSetToCategoria(ResultSet rs) throws SQLException {
        Categoria cat = new Categoria();
        cat.setId(rs.getLong("id"));
        cat.setNombre(rs.getString("nombre"));
        cat.setDescripcion(rs.getString("descripcion"));
        cat.setEliminado(rs.getBoolean("eliminado"));
        cat.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return cat;
    }
}
