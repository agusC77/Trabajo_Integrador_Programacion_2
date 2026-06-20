package integrado.prog2;

import integrado.prog2.config.ConexionDB;
import integrado.prog2.entities.Categoria;
import integrado.prog2.entities.Pedido;
import integrado.prog2.entities.Producto;
import integrado.prog2.entities.Usuario;
import integrado.prog2.enums.Estado;
import integrado.prog2.enums.FormaPago;
import integrado.prog2.enums.Rol;
import integrado.prog2.exception.EntidadNoEncontradaException;
import integrado.prog2.exception.ReglaNegocioException;
import integrado.prog2.exception.StockInvalidoException;
import integrado.prog2.service.CategoriaService;
import integrado.prog2.service.PedidoService;
import integrado.prog2.service.ProductoService;
import integrado.prog2.service.UsuarioService;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TestBusinessRules {
    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println(" INICIANDO VERIFICACIÓN DE REGLAS EN BD  ");
        System.out.println("=========================================");

        // Limpiar base de datos para tener un entorno limpio
        limpiarBaseDeDatos();

        CategoriaService catService = new CategoriaService();
        ProductoService prodService = new ProductoService();
        UsuarioService usrService = new UsuarioService();
        PedidoService pedService = new PedidoService();

        int passed = 0;
        int failed = 0;

        // --- TEST 1: Unicidad de nombre en Categorías ---
        try {
            catService.crear("TestCat", "Desc");
            try {
                catService.crear("TestCat", "Otra desc");
                System.out.println("[FALLÓ] Test 1: Permitió crear dos categorías con el mismo nombre.");
                failed++;
            } catch (ReglaNegocioException e) {
                System.out.println("[PASÓ] Test 1: Evitó nombres duplicados en categorías.");
                passed++;
            }
        } catch (Exception e) {
            System.out.println("[FALLÓ] Test 1 con excepción inesperada: " + e.getMessage());
            failed++;
        }

        // --- TEST 2: Precio negativo en Productos ---
        try {
            Categoria cat = catService.listar().get(0);
            try {
                prodService.crear("ProdNeg", -10.0, "Desc", 5, "img.jpg", true, cat);
                System.out.println("[FALLÓ] Test 2: Permitió crear un producto con precio negativo.");
                failed++;
            } catch (ReglaNegocioException e) {
                System.out.println("[PASÓ] Test 2: Evitó precio negativo en producto.");
                passed++;
            }
        } catch (Exception e) {
            System.out.println("[FALLÓ] Test 2 con excepción inesperada: " + e.getMessage());
            failed++;
        }

        // --- TEST 3: Stock negativo en Productos ---
        try {
            Categoria cat = catService.listar().get(0);
            try {
                prodService.crear("ProdStockNeg", 10.0, "Desc", -5, "img.jpg", true, cat);
                System.out.println("[FALLÓ] Test 3: Permitió crear un producto con stock negativo.");
                failed++;
            } catch (StockInvalidoException e) {
                System.out.println("[PASÓ] Test 3: Evitó stock negativo en producto.");
                passed++;
            }
        } catch (Exception e) {
            System.out.println("[FALLÓ] Test 3 con excepción inesperada: " + e.getMessage());
            failed++;
        }

        // --- TEST 4: Unicidad de Email de Usuario ---
        try {
            usrService.crear("User1", "Last1", "test@test.com", "123", "pass", Rol.USUARIO);
            try {
                usrService.crear("User2", "Last2", "test@test.com", "456", "pass", Rol.USUARIO);
                System.out.println("[FALLÓ] Test 4: Permitió crear dos usuarios con el mismo email.");
                failed++;
            } catch (ReglaNegocioException e) {
                System.out.println("[PASÓ] Test 4: Evitó emails de usuario duplicados.");
                passed++;
            }
        } catch (Exception e) {
            System.out.println("[FALLÓ] Test 4 con excepción inesperada: " + e.getMessage());
            failed++;
        }

        // --- TEST 5: Soft Delete y filtrado de listas ---
        try {
            Categoria catDelete = catService.crear("CatDelete", "Desc");
            Long id = catDelete.getId();
            catService.eliminar(id, null); // En JDBC ya no se usa el segundo parámetro

            try {
                catService.buscarPorId(id);
                System.out.println("[FALLÓ] Test 5: Encontró categoría que fue eliminada lógicamente.");
                failed++;
            } catch (EntidadNoEncontradaException e) {
                boolean inList = catService.listar().stream().anyMatch(c -> c.getId().equals(id));
                if (!inList) {
                    System.out.println("[PASÓ] Test 5: Soft delete oculta la entidad de listados y búsquedas.");
                    passed++;
                } else {
                    System.out.println("[FALLÓ] Test 5: La entidad soft-deleted sigue apareciendo en listar().");
                    failed++;
                }
            }
        } catch (Exception e) {
            System.out.println("[FALLÓ] Test 5 con excepción inesperada: " + e.getMessage());
            failed++;
        }

        // --- TEST 6: Restricción de eliminación de categoría con productos activos ---
        try {
            Categoria catWithProd = catService.crear("CatWithProd", "Desc");
            Producto p = prodService.crear("ProdAsoc", 100.0, "Desc", 5, "img.jpg", true, catWithProd);
            try {
                catService.eliminar(catWithProd.getId(), null);
                System.out.println("[FALLÓ] Test 6: Permitió eliminar categoría con productos activos asociados.");
                failed++;
            } catch (ReglaNegocioException e) {
                System.out.println("[PASÓ] Test 6: Restringió la eliminación de una categoría con productos asociados.");
                passed++;
            }
        } catch (Exception e) {
            System.out.println("[FALLÓ] Test 6 con excepción inesperada: " + e.getMessage());
            failed++;
        }

        // --- TEST 7: Creación de Pedido Transaccional y Deducción de Stock ---
        try {
            Categoria cat = catService.crear("CatPedidos", "Desc");
            Producto prod = prodService.crear("Hamburguesa Especial", 500.0, "Desc", 10, "hamburguesa.jpg", true, cat);
            Usuario usr = usrService.crear("Pedro", "Gómez", "pedro@test.com", "12345", "pass", Rol.USUARIO);

            List<PedidoService.DetalleInput> detalles = new ArrayList<>();
            detalles.add(new PedidoService.DetalleInput(prod, 4));

            Pedido pedido = pedService.crear(usr, FormaPago.TARJETA, detalles);
            
            // Refrescar producto desde DB para ver el stock actualizado
            Producto prodRefrescado = prodService.buscarPorId(prod.getId());

            if (pedido.getTotal() == 2000.0 && prodRefrescado.getStock() == 6) {
                System.out.println("[PASÓ] Test 7: Pedido creado, total calculado y stock descontado en BD correctamente.");
                passed++;
            } else {
                System.out.println("[FALLÓ] Test 7: Total o stock incorrecto en BD. Total: " + pedido.getTotal() + ", Stock en BD: " + prodRefrescado.getStock());
                failed++;
            }
        } catch (Exception e) {
            System.out.println("[FALLÓ] Test 7 con excepción inesperada: " + e.getMessage());
            failed++;
        }

        // --- TEST 8: Cancelación de Pedido transaccional por falta de stock ---
        try {
            Categoria cat = catService.crear("CatPedidos2", "Desc");
            Producto prod = prodService.crear("Papas Fritas", 300.0, "Desc", 5, "papas.jpg", true, cat);
            Usuario usr = usrService.listar().get(0);

            List<PedidoService.DetalleInput> detalles = new ArrayList<>();
            detalles.add(new PedidoService.DetalleInput(prod, 6)); // Pide 6, stock es 5

            try {
                pedService.crear(usr, FormaPago.EFECTIVO, detalles);
                System.out.println("[FALLÓ] Test 8: Permitió crear pedido superando el stock disponible.");
                failed++;
            } catch (StockInvalidoException e) {
                Producto prodRefrescado = prodService.buscarPorId(prod.getId());
                if (prodRefrescado.getStock() == 5) {
                    System.out.println("[PASÓ] Test 8: Canceló creación de pedido por falta de stock y no afectó el stock en BD.");
                    passed++;
                } else {
                    System.out.println("[FALLÓ] Test 8: Lanzó excepción pero el stock en BD se modificó a: " + prodRefrescado.getStock());
                    failed++;
                }
            }
        } catch (Exception e) {
            System.out.println("[FALLÓ] Test 8 con excepción inesperada: " + e.getMessage());
            failed++;
        }

        // Limpiar base de datos después de las pruebas
        limpiarBaseDeDatos();

        System.out.println("=========================================");
        System.out.println("VERIFICACIÓN TERMINADA. PASADOS: " + passed + ", FALLADOS: " + failed);
        System.out.println("=========================================");
    }

    private static void limpiarBaseDeDatos() {
        String[] queries = {
            "DELETE FROM detalle_pedidos",
            "DELETE FROM pedidos",
            "DELETE FROM usuarios",
            "DELETE FROM productos",
            "DELETE FROM categorias"
        };
        try (Connection conn = ConexionDB.obtenerConexion();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            for (String sql : queries) {
                stmt.executeUpdate(sql);
            }
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        } catch (SQLException e) {
            System.out.println("Error al limpiar la base de datos para pruebas: " + e.getMessage());
        }
    }
}
