package integrado.prog2.entities;

import integrado.prog2.exception.ReglaNegocioException;

public class DetallePedido extends Base {
    private int cantidad;
    private Double subtotal;
    private Producto producto;

    public DetallePedido() {
        super();
    }

    public DetallePedido(int cantidad, Double subtotal, Producto producto) {
        super();
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad en el detalle del pedido debe ser mayor a 0.");
        }
        if (producto == null) {
            throw new ReglaNegocioException("El detalle del pedido debe estar asociado a un producto.");
        }
        this.cantidad = cantidad;
        this.subtotal = subtotal;
        this.producto = producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad en el detalle del pedido debe ser mayor a 0.");
        }
        this.cantidad = cantidad;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        if (producto == null) {
            throw new ReglaNegocioException("El detalle del pedido debe estar asociado a un producto.");
        }
        this.producto = producto;
    }

    @Override
    public String toString() {
        String prodName = (producto != null) ? producto.getNombre() : "Invalido";
        Double precioProd = (producto != null) ? producto.getPrecio() : 0.0;
        return String.format("  - %s x%d (Unitario: $%.2f) | Subtotal: $%.2f",
                prodName, cantidad, precioProd, subtotal);
    }
}
