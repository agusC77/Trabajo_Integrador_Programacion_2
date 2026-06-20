package integrado.prog2.entities;

import integrado.prog2.exception.ReglaNegocioException;
import integrado.prog2.exception.StockInvalidoException;

public class Producto extends Base {
    private String nombre;
    private Double precio;
    private String descripcion;
    private int stock;
    private String imagen;
    private Boolean disponible;
    private Categoria categoria;

    public Producto() {
        super();
        this.disponible = true;
    }

    public Producto(String nombre, Double precio, String descripcion, int stock, String imagen, Boolean disponible, Categoria categoria) {
        super();
        if (precio != null && precio < 0) {
            throw new ReglaNegocioException("El precio del producto no puede ser menor a 0.");
        }
        if (stock < 0) {
            throw new StockInvalidoException("El stock del producto no puede ser menor a 0.");
        }
        this.nombre = nombre;
        this.precio = precio;
        this.descripcion = descripcion;
        this.stock = stock;
        this.imagen = imagen;
        this.disponible = disponible;
        this.categoria = categoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre del producto no puede estar vacío.");
        }
        this.nombre = nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        if (precio != null && precio < 0) {
            throw new ReglaNegocioException("El precio del producto no puede ser menor a 0.");
        }
        this.precio = precio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        if (stock < 0) {
            throw new StockInvalidoException("El stock del producto no puede ser menor a 0.");
        }
        this.stock = stock;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Boolean getDisponible() {
        return disponible;
    }

    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        if (categoria == null) {
            throw new ReglaNegocioException("La categoría asociada al producto no puede ser nula.");
        }
        this.categoria = categoria;
    }

    @Override
    public String toString() {
        String catNombre = (categoria != null) ? categoria.getNombre() : "Ninguna";
        return String.format("Producto [ID: %d | Nombre: %s | Precio: $%.2f | Stock: %d | Disponible: %s | Categoría: %s]",
                getId(), nombre, precio, stock, disponible ? "Sí" : "No", catNombre);
    }
}
