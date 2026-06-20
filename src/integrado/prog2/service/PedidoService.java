package integrado.prog2.service;

import integrado.prog2.config.ConexionDB;
import integrado.prog2.entities.Categoria;
import integrado.prog2.entities.DetallePedido;
import integrado.prog2.entities.Pedido;
import integrado.prog2.entities.Producto;
import integrado.prog2.entities.Usuario;
import integrado.prog2.enums.Estado;
import integrado.prog2.enums.FormaPago;
import integrado.prog2.exception.EntidadNoEncontradaException;
import integrado.prog2.exception.NegocioException;
import integrado.prog2.exception.ReglaNegocioException;
import integrado.prog2.exception.StockInvalidoException;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PedidoService {
    private final UsuarioService usuarioService = new UsuarioService();

    public List<Pedido> listar() {
        List<Pedido> lista = new ArrayList<>();
        String sql = "SELECT * FROM pedidos WHERE eliminado = 0";
        try (Connection conn = ConexionDB.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(mapResultSetToPedido(conn, rs));
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al listar pedidos: " + e.getMessage());
        }
        return lista;
    }

    public List<Pedido> listarPorUsuario(Long usuarioId) {
        if (usuarioId == null) {
            throw new ReglaNegocioException("El ID del usuario no puede ser nulo.");
        }
        List<Pedido> lista = new ArrayList<>();
        String sql = "SELECT * FROM pedidos WHERE usuario_id = ? AND eliminado = 0";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, usuarioId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToPedido(conn, rs));
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al listar pedidos por usuario: " + e.getMessage());
        }
        return lista;
    }

    public Pedido buscarPorId(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID del pedido no puede ser nulo.");
        }
        String sql = "SELECT * FROM pedidos WHERE id = ? AND eliminado = 0";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPedido(conn, rs);
                } else {
                    throw new EntidadNoEncontradaException("Pedido no encontrado con ID: " + id);
                }
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al buscar pedido por ID: " + e.getMessage());
        }
    }

    /**
     * Crea un pedido de forma transaccional en la base de datos.
     * Si ocurre algún error de validación o stock, se realiza rollback para mantener consistencia.
     */
    public Pedido crear(Usuario usuario, FormaPago formaPago, List<DetalleInput> detallesInput) {
        if (usuario == null || usuario.isEliminado()) {
            throw new ReglaNegocioException("El usuario seleccionado no es válido o está eliminado.");
        }
        if (detallesInput == null || detallesInput.isEmpty()) {
            throw new ReglaNegocioException("El pedido debe contener al menos un detalle.");
        }

        String insertPedidoSql = "INSERT INTO pedidos (fecha, estado, total, forma_pago, usuario_id, eliminado) VALUES (?, ?, ?, ?, ?, 0)";
        String selectProductSql = "SELECT stock, nombre, disponible, eliminado, precio FROM productos WHERE id = ? AND eliminado = 0 FOR UPDATE";
        String updateStockSql = "UPDATE productos SET stock = stock - ? WHERE id = ?";
        String insertDetailSql = "INSERT INTO detalle_pedidos (cantidad, subtotal, producto_id, pedido_id, eliminado) VALUES (?, ?, ?, ?, 0)";
        String updatePedidoTotalSql = "UPDATE pedidos SET total = ? WHERE id = ?";

        Connection conn = null;
        Long pedidoId = null;

        try {
            conn = ConexionDB.obtenerConexion();
            conn.setAutoCommit(false); // Iniciar transacción

            // 1. Insertar el Pedido principal
            try (PreparedStatement insertPedidoPstmt = conn.prepareStatement(insertPedidoSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPedidoPstmt.setDate(1, Date.valueOf(LocalDate.now()));
                insertPedidoPstmt.setString(2, Estado.PENDIENTE.name());
                insertPedidoPstmt.setDouble(3, 0.0); // Total provisional
                insertPedidoPstmt.setString(4, formaPago.name());
                insertPedidoPstmt.setLong(5, usuario.getId());
                insertPedidoPstmt.executeUpdate();

                try (ResultSet generatedKeys = insertPedidoPstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        pedidoId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Error al obtener ID autogenerado del pedido.");
                    }
                }
            }

            double totalPedido = 0.0;

            // 2. Procesar detalles e insertar
            for (DetalleInput input : detallesInput) {
                Producto prod = input.producto;
                int cantidad = input.cantidad;

                if (cantidad <= 0) {
                    throw new ReglaNegocioException("La cantidad debe ser mayor a 0.");
                }

                // Obtener stock y datos actuales del producto desde la base de datos con bloqueo de fila
                int currentStock = 0;
                double prodPrecio = 0.0;
                boolean disponible = false;
                String prodNombre = "";

                try (PreparedStatement selectProductPstmt = conn.prepareStatement(selectProductSql)) {
                    selectProductPstmt.setLong(1, prod.getId());
                    try (ResultSet rs = selectProductPstmt.executeQuery()) {
                        if (rs.next()) {
                            currentStock = rs.getInt("stock");
                            prodNombre = rs.getString("nombre");
                            disponible = rs.getBoolean("disponible");
                            prodPrecio = rs.getDouble("precio");
                        } else {
                            throw new EntidadNoEncontradaException("Producto " + prod.getNombre() + " no encontrado en la BD.");
                        }
                    }
                }

                // Validar stock y disponibilidad
                if (!disponible) {
                    throw new ReglaNegocioException("El producto " + prodNombre + " no está disponible para venta.");
                }
                if (currentStock < cantidad) {
                    throw new StockInvalidoException(String.format("Stock insuficiente para el producto: %s. Stock disponible: %d, solicitado: %d",
                            prodNombre, currentStock, cantidad));
                }

                double subtotal = cantidad * prodPrecio;
                totalPedido += subtotal;

                // Restar stock del producto en la base de datos
                try (PreparedStatement updateStockPstmt = conn.prepareStatement(updateStockSql)) {
                    updateStockPstmt.setInt(1, cantidad);
                    updateStockPstmt.setLong(2, prod.getId());
                    updateStockPstmt.executeUpdate();
                }

                // Insertar detalle del pedido
                try (PreparedStatement insertDetailPstmt = conn.prepareStatement(insertDetailSql)) {
                    insertDetailPstmt.setInt(1, cantidad);
                    insertDetailPstmt.setDouble(2, subtotal);
                    insertDetailPstmt.setLong(3, prod.getId());
                    insertDetailPstmt.setLong(4, pedidoId);
                    insertDetailPstmt.executeUpdate();
                }
            }

            // 3. Actualizar el total final del Pedido
            try (PreparedStatement updatePedidoTotalPstmt = conn.prepareStatement(updatePedidoTotalSql)) {
                updatePedidoTotalPstmt.setDouble(1, totalPedido);
                updatePedidoTotalPstmt.setLong(2, pedidoId);
                updatePedidoTotalPstmt.executeUpdate();
            }

            conn.commit(); // Confirmar transacción
            return buscarPorId(pedidoId);

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Deshacer cambios en caso de error
                } catch (SQLException ex) {
                    // Ignorar
                }
            }
            if (e instanceof NegocioException) {
                throw (NegocioException) e;
            } else {
                throw new ReglaNegocioException("Error al crear el pedido de forma transaccional: " + e.getMessage());
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // Ignorar
                }
            }
        }
    }

    /**
     * Actualiza el estado y forma de pago del pedido en la base de datos de manera transaccional.
     * Devuelve o descuenta el stock de los productos según las transiciones desde/hacia CANCELADO.
     */
    public void actualizarEstadoYFormaPago(Long id, Estado nuevoEstado, FormaPago nuevaFormaPago) {
        Pedido p = buscarPorId(id); // Valida que exista
        Estado anteriorEstado = p.getEstado();

        String selectDetailsSql = "SELECT cantidad, producto_id FROM detalle_pedidos WHERE pedido_id = ? AND eliminado = 0";
        String selectProductStockSql = "SELECT stock, nombre FROM productos WHERE id = ? FOR UPDATE";
        String updateStockSql = "UPDATE productos SET stock = stock + ? WHERE id = ?"; // + o - depende del signo

        Connection conn = null;
        try {
            conn = ConexionDB.obtenerConexion();
            conn.setAutoCommit(false); // Iniciar transacción

            if (nuevaFormaPago != null) {
                String sql = "UPDATE pedidos SET forma_pago = ? WHERE id = ? AND eliminado = 0";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, nuevaFormaPago.name());
                    pstmt.setLong(2, id);
                    pstmt.executeUpdate();
                }
            }

            if (nuevoEstado != null && !nuevoEstado.equals(anteriorEstado)) {
                // Caso 1: Transición hacia CANCELADO (devolver stock)
                if (nuevoEstado == Estado.CANCELADO && anteriorEstado != Estado.CANCELADO) {
                    try (PreparedStatement selectDetailsPstmt = conn.prepareStatement(selectDetailsSql)) {
                        selectDetailsPstmt.setLong(1, id);
                        try (ResultSet rs = selectDetailsPstmt.executeQuery()) {
                            while (rs.next()) {
                                int cantidad = rs.getInt("cantidad");
                                long prodId = rs.getLong("producto_id");

                                try (PreparedStatement updateStockPstmt = conn.prepareStatement(updateStockSql)) {
                                    updateStockPstmt.setInt(1, cantidad);
                                    updateStockPstmt.setLong(2, prodId);
                                    updateStockPstmt.executeUpdate();
                                }
                            }
                        }
                    }
                }
                // Caso 2: Transición desde CANCELADO hacia activo (volver a descontar stock)
                else if (anteriorEstado == Estado.CANCELADO && nuevoEstado != Estado.CANCELADO) {
                    // Validar primero disponibilidad y descontar
                    try (PreparedStatement selectDetailsPstmt = conn.prepareStatement(selectDetailsSql)) {
                        selectDetailsPstmt.setLong(1, id);
                        try (ResultSet rs = selectDetailsPstmt.executeQuery()) {
                            while (rs.next()) {
                                int cantidad = rs.getInt("cantidad");
                                long prodId = rs.getLong("producto_id");

                                // Lockear y verificar stock del producto
                                int currentStock = 0;
                                String prodNombre = "";
                                try (PreparedStatement selectProductStockPstmt = conn.prepareStatement(selectProductStockSql)) {
                                    selectProductStockPstmt.setLong(1, prodId);
                                    try (ResultSet rsProd = selectProductStockPstmt.executeQuery()) {
                                        if (rsProd.next()) {
                                            currentStock = rsProd.getInt("stock");
                                            prodNombre = rsProd.getString("nombre");
                                        }
                                    }
                                }

                                if (currentStock < cantidad) {
                                    throw new StockInvalidoException(String.format("No se puede reactivar el pedido. Stock insuficiente para el producto: %s (Stock: %d, Requerido: %d)",
                                            prodNombre, currentStock, cantidad));
                                }

                                // Descontar stock
                                try (PreparedStatement updateStockPstmt = conn.prepareStatement(updateStockSql)) {
                                    updateStockPstmt.setInt(1, -cantidad); // Restar
                                    updateStockPstmt.setLong(2, prodId);
                                    updateStockPstmt.executeUpdate();
                                }
                            }
                        }
                    }
                }

                // Guardar el nuevo estado en la base de datos
                String sql = "UPDATE pedidos SET estado = ? WHERE id = ? AND eliminado = 0";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, nuevoEstado.name());
                    pstmt.setLong(2, id);
                    pstmt.executeUpdate();
                }
            }

            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // Ignorar
                }
            }
            if (e instanceof NegocioException) {
                throw (NegocioException) e;
            } else {
                throw new ReglaNegocioException("Error al actualizar el estado del pedido: " + e.getMessage());
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // Ignorar
                }
            }
        }
    }

    /**
     * Elimina lógicamente un pedido y sus detalles de manera transaccional.
     * Devuelve el stock si el pedido no estaba CANCELADO al momento de eliminarse.
     */
    public void eliminar(Long id) {
        Pedido p = buscarPorId(id); // Valida que exista y no esté eliminado

        String selectDetailsSql = "SELECT cantidad, producto_id FROM detalle_pedidos WHERE pedido_id = ? AND eliminado = 0";
        String updateStockSql = "UPDATE productos SET stock = stock + ? WHERE id = ?";
        String deletePedidoSql = "UPDATE pedidos SET eliminado = 1 WHERE id = ?";
        String deleteDetailsSql = "UPDATE detalle_pedidos SET eliminado = 1 WHERE pedido_id = ?";

        Connection conn = null;
        try {
            conn = ConexionDB.obtenerConexion();
            conn.setAutoCommit(false);

            // Devolver stock si no estaba cancelado
            if (p.getEstado() != Estado.CANCELADO) {
                try (PreparedStatement selectDetailsPstmt = conn.prepareStatement(selectDetailsSql)) {
                    selectDetailsPstmt.setLong(1, id);
                    try (ResultSet rs = selectDetailsPstmt.executeQuery()) {
                        while (rs.next()) {
                            int cantidad = rs.getInt("cantidad");
                            long prodId = rs.getLong("producto_id");

                            try (PreparedStatement updateStockPstmt = conn.prepareStatement(updateStockSql)) {
                                updateStockPstmt.setInt(1, cantidad);
                                updateStockPstmt.setLong(2, prodId);
                                updateStockPstmt.executeUpdate();
                            }
                        }
                    }
                }
            }

            // Marcar eliminado = true en Pedido
            try (PreparedStatement deletePedidoPstmt = conn.prepareStatement(deletePedidoSql)) {
                deletePedidoPstmt.setLong(1, id);
                deletePedidoPstmt.executeUpdate();
            }

            // Marcar eliminado = true en DetallePedidos
            try (PreparedStatement deleteDetailsPstmt = conn.prepareStatement(deleteDetailsSql)) {
                deleteDetailsPstmt.setLong(1, id);
                deleteDetailsPstmt.executeUpdate();
            }

            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // Ignorar
                }
            }
            throw new ReglaNegocioException("Error al eliminar pedido de la base de datos: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // Ignorar
                }
            }
        }
    }

    private Pedido mapResultSetToPedido(Connection conn, ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setId(rs.getLong("id"));
        p.setFecha(rs.getDate("fecha").toLocalDate());
        p.setEstado(Estado.valueOf(rs.getString("estado")));
        p.setTotal(rs.getDouble("total"));
        p.setFormaPago(FormaPago.valueOf(rs.getString("forma_pago")));
        p.setEliminado(rs.getBoolean("eliminado"));
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        // Cargar Usuario asociado
        long usuarioId = rs.getLong("usuario_id");
        p.setUsuario(usuarioService.buscarPorId(usuarioId));

        // Cargar detalles desde base de datos
        p.setDetalles(fetchDetallesForPedido(conn, p.getId()));
        return p;
    }

    private List<DetallePedido> fetchDetallesForPedido(Connection conn, Long pedidoId) throws SQLException {
        List<DetallePedido> detalles = new ArrayList<>();
        String sql = "SELECT dp.*, " +
                "p.nombre AS prod_nombre, p.precio AS prod_precio, p.descripcion AS prod_desc, p.stock AS prod_stock, p.imagen AS prod_img, p.disponible AS prod_disp, p.categoria_id AS prod_cat_id, p.eliminado AS prod_elim, p.created_at AS prod_created, " +
                "c.nombre AS cat_nombre, c.descripcion AS cat_desc, c.eliminado AS cat_elim, c.created_at AS cat_created " +
                "FROM detalle_pedidos dp " +
                "JOIN productos p ON dp.producto_id = p.id " +
                "JOIN categorias c ON p.categoria_id = c.id " +
                "WHERE dp.pedido_id = ? AND dp.eliminado = 0";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, pedidoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Categoria cat = new Categoria();
                    cat.setId(rs.getLong("prod_cat_id"));
                    cat.setNombre(rs.getString("cat_nombre"));
                    cat.setDescripcion(rs.getString("cat_desc"));
                    cat.setEliminado(rs.getBoolean("cat_elim"));
                    cat.setCreatedAt(rs.getTimestamp("cat_created").toLocalDateTime());

                    Producto prod = new Producto();
                    prod.setId(rs.getLong("producto_id"));
                    prod.setNombre(rs.getString("prod_nombre"));
                    prod.setPrecio(rs.getDouble("prod_precio"));
                    prod.setDescripcion(rs.getString("prod_desc"));
                    prod.setStock(rs.getInt("prod_stock"));
                    prod.setImagen(rs.getString("prod_img"));
                    prod.setDisponible(rs.getBoolean("prod_disp"));
                    prod.setEliminado(rs.getBoolean("prod_elim"));
                    prod.setCreatedAt(rs.getTimestamp("prod_created").toLocalDateTime());
                    prod.setCategoria(cat);

                    DetallePedido det = new DetallePedido();
                    det.setId(rs.getLong("id"));
                    det.setCantidad(rs.getInt("cantidad"));
                    det.setSubtotal(rs.getDouble("subtotal"));
                    det.setEliminado(rs.getBoolean("eliminado"));
                    det.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    det.setProducto(prod);

                    detalles.add(det);
                }
            }
        }
        return detalles;
    }

    public static class DetalleInput {
        public Producto producto;
        public int cantidad;

        public DetalleInput(Producto producto, int cantidad) {
            this.producto = producto;
            this.cantidad = cantidad;
        }
    }
}
