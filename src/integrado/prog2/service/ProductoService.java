package integrado.prog2.service;

import integrado.prog2.config.ConexionDB;
import integrado.prog2.entities.Categoria;
import integrado.prog2.entities.Producto;
import integrado.prog2.exception.EntidadNoEncontradaException;
import integrado.prog2.exception.ReglaNegocioException;
import integrado.prog2.exception.StockInvalidoException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoService {

    public List<Producto> listar() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.*, c.nombre AS cat_nombre, c.descripcion AS cat_desc, c.eliminado AS cat_elim, c.created_at AS cat_created " +
                "FROM productos p " +
                "JOIN categorias c ON p.categoria_id = c.id " +
                "WHERE p.eliminado = 0";
        try (Connection conn = ConexionDB.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(mapResultSetToProducto(rs));
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al listar productos: " + e.getMessage());
        }
        return lista;
    }

    public List<Producto> listarPorCategoria(Long categoriaId) {
        if (categoriaId == null) {
            throw new ReglaNegocioException("El ID de la categoría no puede ser nulo.");
        }
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.*, c.nombre AS cat_nombre, c.descripcion AS cat_desc, c.eliminado AS cat_elim, c.created_at AS cat_created " +
                "FROM productos p " +
                "JOIN categorias c ON p.categoria_id = c.id " +
                "WHERE p.categoria_id = ? AND p.eliminado = 0";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, categoriaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToProducto(rs));
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al listar productos por categoría: " + e.getMessage());
        }
        return lista;
    }

    public Producto buscarPorId(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del producto no puede ser nulo.");
        }
        String sql = "SELECT p.*, c.nombre AS cat_nombre, c.descripcion AS cat_desc, c.eliminado AS cat_elim, c.created_at AS cat_created " +
                "FROM productos p " +
                "JOIN categorias c ON p.categoria_id = c.id " +
                "WHERE p.id = ? AND p.eliminado = 0";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProducto(rs);
                } else {
                    throw new EntidadNoEncontradaException("Producto no encontrado con ID: " + id);
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al buscar producto por ID: " + e.getMessage());
        }
    }

    public Producto crear(String nombre, Double precio, String descripcion, int stock, String imagen, Boolean disponible, Categoria categoria) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre del producto no puede estar vacío.");
        }
        if (precio == null || precio < 0) {
            throw new ReglaNegocioException("El precio del producto no puede ser menor a 0.");
        }
        if (stock < 0) {
            throw new StockInvalidoException("El stock del producto no puede ser menor a 0.");
        }
        if (categoria == null || categoria.isEliminado()) {
            throw new ReglaNegocioException("La categoría asociada no es válida o está eliminada.");
        }

        String sql = "INSERT INTO productos (nombre, precio, descripcion, stock, imagen, disponible, categoria_id, eliminado) VALUES (?, ?, ?, ?, ?, ?, ?, 0)";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nombre.trim());
            pstmt.setDouble(2, precio);
            pstmt.setString(3, descripcion != null ? descripcion.trim() : "");
            pstmt.setInt(4, stock);
            pstmt.setString(5, imagen != null ? imagen.trim() : "");
            pstmt.setBoolean(6, disponible != null ? disponible : true);
            pstmt.setLong(7, categoria.getId());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long generatedId = generatedKeys.getLong(1);
                    return buscarPorId(generatedId);
                } else {
                    throw new ReglaNegocioException("Error al obtener el ID generado del producto.");
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al crear producto en la base de datos: " + e.getMessage());
        }
    }

    public Producto editar(Long id, String nombre, Double precio, String descripcion, Integer stock, String imagen, Boolean disponible, Categoria categoria) {
        Producto actual = buscarPorId(id); // Valida existencia

        // Mantener anteriores si los nuevos parámetros son nulos o vacíos
        String nombreUpdate = (nombre != null && !nombre.trim().isEmpty()) ? nombre.trim() : actual.getNombre();
        Double precioUpdate = (precio != null) ? precio : actual.getPrecio();
        String descUpdate = (descripcion != null) ? descripcion.trim() : actual.getDescripcion();
        Integer stockUpdate = (stock != null) ? stock : actual.getStock();
        String imgUpdate = (imagen != null) ? imagen.trim() : actual.getImagen();
        Boolean dispUpdate = (disponible != null) ? disponible : actual.getDisponible();
        Long catIdUpdate = (categoria != null) ? categoria.getId() : actual.getCategoria().getId();

        // Validaciones
        if (precioUpdate < 0) {
            throw new ReglaNegocioException("El precio del producto no puede ser menor a 0.");
        }
        if (stockUpdate < 0) {
            throw new StockInvalidoException("El stock del producto no puede ser menor a 0.");
        }

        String sql = "UPDATE productos SET nombre = ?, precio = ?, descripcion = ?, stock = ?, imagen = ?, disponible = ?, categoria_id = ? " +
                "WHERE id = ? AND eliminado = 0";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nombreUpdate);
            pstmt.setDouble(2, precioUpdate);
            pstmt.setString(3, descUpdate);
            pstmt.setInt(4, stockUpdate);
            pstmt.setString(5, imgUpdate);
            pstmt.setBoolean(6, dispUpdate);
            pstmt.setLong(7, catIdUpdate);
            pstmt.setLong(8, id);
            pstmt.executeUpdate();

            return buscarPorId(id);
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al editar producto en la base de datos: " + e.getMessage());
        }
    }

    public void eliminar(Long id) {
        buscarPorId(id); // Valida que exista y no esté eliminado
        String sql = "UPDATE productos SET eliminado = 1 WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al eliminar producto en la base de datos: " + e.getMessage());
        }
    }

    private Producto mapResultSetToProducto(ResultSet rs) throws SQLException {
        Categoria cat = new Categoria();
        cat.setId(rs.getLong("categoria_id"));
        cat.setNombre(rs.getString("cat_nombre"));
        cat.setDescripcion(rs.getString("cat_desc"));
        cat.setEliminado(rs.getBoolean("cat_elim"));
        cat.setCreatedAt(rs.getTimestamp("cat_created").toLocalDateTime());

        Producto p = new Producto();
        p.setId(rs.getLong("id"));
        p.setNombre(rs.getString("nombre"));
        p.setPrecio(rs.getDouble("precio"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setStock(rs.getInt("stock"));
        p.setImagen(rs.getString("imagen"));
        p.setDisponible(rs.getBoolean("disponible"));
        p.setEliminado(rs.getBoolean("eliminado"));
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        p.setCategoria(cat);
        return p;
    }
}
