package integrado.prog2.service;

import integrado.prog2.config.ConexionDB;
import integrado.prog2.entities.Usuario;
import integrado.prog2.enums.Rol;
import integrado.prog2.exception.EntidadNoEncontradaException;
import integrado.prog2.exception.ReglaNegocioException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioService {

    public List<Usuario> listar() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE eliminado = 0";
        try (Connection conn = ConexionDB.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(mapResultSetToUsuario(rs));
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al listar usuarios: " + e.getMessage());
        }
        return lista;
    }

    public Usuario buscarPorId(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del usuario no puede ser nulo.");
        }
        String sql = "SELECT * FROM usuarios WHERE id = ? AND eliminado = 0";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUsuario(rs);
                } else {
                    throw new EntidadNoEncontradaException("Usuario no encontrado con ID: " + id);
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al buscar usuario por ID: " + e.getMessage());
        }
    }

    public Usuario crear(String nombre, String apellido, String mail, String celular, String contrasena, Rol rol) {
        if (nombre == null || nombre.trim().isEmpty() ||
                apellido == null || apellido.trim().isEmpty() ||
                mail == null || mail.trim().isEmpty()) {
            throw new ReglaNegocioException("Nombre, apellido y mail son obligatorios.");
        }

        String checkSql = "SELECT COUNT(*) FROM usuarios WHERE mail = ? AND eliminado = 0";
        String insertSql = "INSERT INTO usuarios (nombre, apellido, mail, celular, contrasena, rol, eliminado) VALUES (?, ?, ?, ?, ?, ?, 0)";

        try (Connection conn = ConexionDB.obtenerConexion()) {
            // Verificar unicidad de email
            try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                checkPstmt.setString(1, mail.trim());
                try (ResultSet rs = checkPstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new ReglaNegocioException("Ya existe un usuario registrado con el email: " + mail.trim());
                    }
                }
            }

            // Insertar usuario
            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPstmt.setString(1, nombre.trim());
                insertPstmt.setString(2, apellido.trim());
                insertPstmt.setString(3, mail.trim());
                insertPstmt.setString(4, celular != null ? celular.trim() : "");
                insertPstmt.setString(5, contrasena != null ? contrasena : "");
                insertPstmt.setString(6, (rol != null ? rol : Rol.USUARIO).name());
                insertPstmt.executeUpdate();

                try (ResultSet generatedKeys = insertPstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Long generatedId = generatedKeys.getLong(1);
                        return buscarPorId(generatedId);
                    } else {
                        throw new ReglaNegocioException("Error al obtener el ID generado del usuario.");
                    }
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al crear usuario en la base de datos: " + e.getMessage());
        }
    }

    public Usuario editar(Long id, String nombre, String apellido, String mail, String celular, String contrasena, Rol rol) {
        Usuario actual = buscarPorId(id); // Valida existencia

        String nombreUpdate = (nombre != null && !nombre.trim().isEmpty()) ? nombre.trim() : actual.getNombre();
        String apellidoUpdate = (apellido != null && !apellido.trim().isEmpty()) ? apellido.trim() : actual.getApellido();
        String mailUpdate = (mail != null && !mail.trim().isEmpty()) ? mail.trim() : actual.getMail();
        String celularUpdate = (celular != null) ? celular.trim() : actual.getCelular();
        String contrasenaUpdate = (contrasena != null && !contrasena.isEmpty()) ? contrasena : actual.getContraseña();
        Rol rolUpdate = (rol != null) ? rol : actual.getRol();

        String checkSql = "SELECT COUNT(*) FROM usuarios WHERE mail = ? AND id != ? AND eliminado = 0";
        String updateSql = "UPDATE usuarios SET nombre = ?, apellido = ?, mail = ?, celular = ?, contrasena = ?, rol = ? WHERE id = ? AND eliminado = 0";

        try (Connection conn = ConexionDB.obtenerConexion()) {
            // Verificar unicidad de email contra otros usuarios
            try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                checkPstmt.setString(1, mailUpdate);
                checkPstmt.setLong(2, id);
                try (ResultSet rs = checkPstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new ReglaNegocioException("Ya existe otro usuario registrado con el email: " + mailUpdate);
                    }
                }
            }

            // Actualizar usuario
            try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                updatePstmt.setString(1, nombreUpdate);
                updatePstmt.setString(2, apellidoUpdate);
                updatePstmt.setString(3, mailUpdate);
                updatePstmt.setString(4, celularUpdate);
                updatePstmt.setString(5, contrasenaUpdate);
                updatePstmt.setString(6, rolUpdate.name());
                updatePstmt.setLong(7, id);
                updatePstmt.executeUpdate();

                return buscarPorId(id);
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al editar usuario en la base de datos: " + e.getMessage());
        }
    }

    public void eliminar(Long id) {
        buscarPorId(id); // Valida que exista y no esté eliminado
        String sql = "UPDATE usuarios SET eliminado = 1 WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al eliminar usuario en la base de datos: " + e.getMessage());
        }
    }

    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getLong("id"));
        u.setNombre(rs.getString("nombre"));
        u.setApellido(rs.getString("apellido"));
        u.setMail(rs.getString("mail"));
        u.setCelular(rs.getString("celular"));
        u.setContraseña(rs.getString("contrasena"));
        u.setRol(Rol.valueOf(rs.getString("rol")));
        u.setEliminado(rs.getBoolean("eliminado"));
        u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return u;
    }
}
