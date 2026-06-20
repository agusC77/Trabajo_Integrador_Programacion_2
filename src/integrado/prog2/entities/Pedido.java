package integrado.prog2.entities;

import integrado.prog2.enums.Estado;
import integrado.prog2.enums.FormaPago;
import integrado.prog2.interfaces.Calculable;
import integrado.prog2.exception.StockInvalidoException;
import integrado.prog2.exception.ReglaNegocioException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Pedido extends Base implements Calculable {
    private LocalDate fecha;
    private Estado estado;
    private Double total;
    private FormaPago formaPago;
    private Usuario usuario;
    private List<DetallePedido> detalles;

    public Pedido() {
        super();
        this.fecha = LocalDate.now();
        this.estado = Estado.PENDIENTE;
        this.total = 0.0;
        this.detalles = new ArrayList<>();
    }

    public Pedido(Usuario usuario, FormaPago formaPago) {
        super();
        if (usuario == null) {
            throw new ReglaNegocioException("No se puede crear un pedido sin un usuario asociado.");
        }
        this.fecha = LocalDate.now();
        this.estado = Estado.PENDIENTE;
        this.total = 0.0;
        this.formaPago = formaPago;
        this.usuario = usuario;
        this.detalles = new ArrayList<>();
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(FormaPago formaPago) {
        this.formaPago = formaPago;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new ReglaNegocioException("El pedido debe estar asociado a un usuario.");
        }
        this.usuario = usuario;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePedido> detalles) {
        this.detalles = detalles;
    }

    public void addDetallePedido(int cantidad, Double subtotal, Producto producto) {
        if (producto == null) {
            throw new ReglaNegocioException("El producto asociado al detalle no puede ser nulo.");
        }
        if (producto.isEliminado() || !producto.getDisponible()) {
            throw new ReglaNegocioException("El producto " + producto.getNombre() + " no está disponible.");
        }
        if (producto.getStock() < cantidad) {
            throw new StockInvalidoException(String.format("Stock insuficiente para el producto: %s. Stock disponible: %d, solicitado: %d",
                    producto.getNombre(), producto.getStock(), cantidad));
        }

        DetallePedido detalle = new DetallePedido(cantidad, subtotal, producto);
        this.detalles.add(detalle);
    }

    public DetallePedido findeDetallePedidoByProducto(Producto producto) {
        if (producto == null) return null;
        for (DetallePedido detalle : detalles) {
            if (detalle.getProducto() != null && detalle.getProducto().equals(producto)) {
                return detalle;
            }
        }
        return null;
    }

    public void deleteDetallePedidoByProducto(Producto producto) {
        DetallePedido detalle = findeDetallePedidoByProducto(producto);
        if (detalle != null) {
            detalles.remove(detalle);
        }
    }

    @Override
    public void calcularTotal() {
        double calculatedTotal = 0.0;
        for (DetallePedido detalle : detalles) {
            if (detalle.getSubtotal() != null) {
                calculatedTotal += detalle.getSubtotal();
            }
        }
        this.total = calculatedTotal;
    }

    @Override
    public String toString() {
        String userNombre = (usuario != null) ? (usuario.getNombre() + " " + usuario.getApellido()) : "Ninguno";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Pedido [ID: %d | Fecha: %s | Cliente: %s | Estado: %s | Forma de Pago: %s | Total: $%.2f]\n",
                getId(), fecha, userNombre, estado, formaPago, total));
        sb.append("Detalles:\n");
        if (detalles.isEmpty()) {
            sb.append("  (Sin detalles)\n");
        } else {
            for (DetallePedido det : detalles) {
                sb.append(det.toString()).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
