package integrado.prog2;

import integrado.prog2.config.ConexionDB;
import integrado.prog2.entities.Categoria;
import integrado.prog2.entities.Pedido;
import integrado.prog2.entities.Producto;
import integrado.prog2.entities.Usuario;
import integrado.prog2.enums.Estado;
import integrado.prog2.enums.FormaPago;
import integrado.prog2.enums.Rol;
import integrado.prog2.exception.NegocioException;
import integrado.prog2.service.CategoriaService;
import integrado.prog2.service.PedidoService;
import integrado.prog2.service.ProductoService;
import integrado.prog2.service.UsuarioService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final CategoriaService catService = new CategoriaService();
    private static final ProductoService prodService = new ProductoService();
    private static final UsuarioService usrService = new UsuarioService();
    private static final PedidoService pedService = new PedidoService();

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("        FOOD STORE - SISTEMA DE PEDIDOS (JDBC)   ");
        System.out.println("=================================================");
        System.out.println("[DB] Probando conexión a la base de datos...");

        try (Connection conn = ConexionDB.obtenerConexion()) {
            System.out.println("[DB] Conexión establecida con éxito a la base de datos 'foodstore'.");
        } catch (SQLException e) {
            System.out.println("\n[ERROR CRÍTICO DE CONEXIÓN]");
            System.out.println("No se pudo conectar a la base de datos MySQL.");
            System.out.println("Detalle del error: " + e.getMessage());
            System.out.println("\nPor favor verifique que:");
            System.out.println("1. Su servidor MySQL esté corriendo (XAMPP, WampServer o servicio local).");
            System.out.println("2. Haya ejecutado el script 'schema.sql' para crear la base de datos 'foodstore' y sus tablas.");
            System.out.println("3. Las credenciales en 'ConexionDB.java' coincidan con su servidor MySQL.");
            System.out.println("\nEl programa finalizará ahora.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        boolean salir = false;

        System.out.println("=================================================");
        System.out.println("        FOOD STORE - INICIALIZADO CON ÉXITO      ");
        System.out.println("=================================================");

        while (!salir) {
            try {
                mostrarMenuPrincipal();
                int opcion = readInt(scanner, "Seleccione una opción: ", true, 0, 4);
                switch (opcion) {
                    case 1 -> menuCategorias(scanner);
                    case 2 -> menuProductos(scanner);
                    case 3 -> menuUsuarios(scanner);
                    case 4 -> menuPedidos(scanner);
                    case 0 -> {
                        System.out.println("\n¡Gracias por utilizar Food Store! Saliendo del sistema...");
                        salir = true;
                    }
                }
            } catch (Exception e) {
                System.out.println("\n[ERROR CRÍTICO INESPERADO] " + e.getMessage());
            }
        }
        scanner.close();
    }

    private static void mostrarMenuPrincipal() {
        System.out.println("\n=================================================");
        System.out.println("         === FOOD STORE - MENÚ PRINCIPAL ===     ");
        System.out.println("=================================================");
        System.out.println("1. Gestión de Categorías");
        System.out.println("2. Gestión de Productos");
        System.out.println("3. Gestión de Usuarios");
        System.out.println("4. Gestión de Pedidos");
        System.out.println("0. Salir");
        System.out.println("=================================================");
    }

    // ==========================================
    // MENÚ CATEGORÍAS
    // ==========================================
    private static void menuCategorias(Scanner scanner) {
        boolean volver = false;
        while (!volver) {
            try {
                System.out.println("\n--- SUBMENÚ GESTIÓN DE CATEGORÍAS ---");
                System.out.println("1. Listar categorías");
                System.out.println("2. Crear categoría");
                System.out.println("3. Editar categoría");
                System.out.println("4. Eliminar categoría (baja lógica)");
                System.out.println("0. Volver");
                int opcion = readInt(scanner, "Seleccione una opción: ", true, 0, 4);

                switch (opcion) {
                    case 1 -> listarCategorias();
                    case 2 -> crearCategoria(scanner);
                    case 3 -> editarCategoria(scanner);
                    case 4 -> eliminarCategoria(scanner);
                    case 0 -> volver = true;
                }
            } catch (NegocioException e) {
                System.out.println("\n[ERROR DE NEGOCIO] " + e.getMessage());
            }
        }
    }

    private static void listarCategorias() {
        System.out.println("\n--- LISTADO DE CATEGORÍAS ACTIVAS ---");
        List<Categoria> lista = catService.listar();
        if (lista.isEmpty()) {
            System.out.println("No hay categorías cargadas.");
        } else {
            lista.forEach(System.out::println);
        }
    }

    private static void crearCategoria(Scanner scanner) {
        System.out.println("\n--- NUEVA CATEGORÍA ---");
        String nombre = readString(scanner, "Nombre: ", true);
        String desc = readString(scanner, "Descripción: ", false);
        Categoria nueva = catService.crear(nombre, desc);
        System.out.println("\n[ÉXITO] Categoría creada correctamente. ID asignado: " + nueva.getId());
    }

    private static void editarCategoria(Scanner scanner) {
        System.out.println("\n--- EDITAR CATEGORÍA ---");
        listarCategorias();
        Long id = readLong(scanner, "Ingrese el ID de la categoría a editar: ", true);
        Categoria actual = catService.buscarPorId(id);

        System.out.println("\nEditando categoría: " + actual.getNombre());
        String nombre = readString(scanner, "Nuevo nombre (Enter para mantener [" + actual.getNombre() + "]): ", false);
        if (nombre.isEmpty()) {
            nombre = actual.getNombre();
        }
        String desc = readString(scanner, "Nueva descripción (Enter para mantener [" + actual.getDescripcion() + "]): ", false);
        if (desc.isEmpty()) {
            desc = actual.getDescripcion();
        }

        catService.editar(id, nombre, desc);
        System.out.println("\n[ÉXITO] Categoría editada con éxito.");
    }

    private static void eliminarCategoria(Scanner scanner) {
        System.out.println("\n--- ELIMINAR CATEGORÍA ---");
        listarCategorias();
        Long id = readLong(scanner, "Ingrese el ID de la categoría a eliminar: ", true);
        Categoria actual = catService.buscarPorId(id);

        System.out.println("\nCategoría seleccionada: " + actual.getNombre());
        String confirm = readString(scanner, "¿Está seguro que desea eliminar la categoría? (S/N): ", true);
        if (confirm.equalsIgnoreCase("s")) {
            // We pass null for the list since the service now checks DB directly
            catService.eliminar(id, null);
            System.out.println("\n[ÉXITO] Categoría eliminada correctamente (baja lógica).");
        } else {
            System.out.println("\nOperación cancelada.");
        }
    }

    // ==========================================
    // MENÚ PRODUCTOS
    // ==========================================
    private static void menuProductos(Scanner scanner) {
        boolean volver = false;
        while (!volver) {
            try {
                System.out.println("\n--- SUBMENÚ GESTIÓN DE PRODUCTOS ---");
                System.out.println("1. Listar productos (general)");
                System.out.println("2. Listar productos por categoría");
                System.out.println("3. Crear producto");
                System.out.println("4. Editar producto");
                System.out.println("5. Eliminar producto (baja lógica)");
                System.out.println("0. Volver");
                int opcion = readInt(scanner, "Seleccione una opción: ", true, 0, 5);

                switch (opcion) {
                    case 1 -> listarProductos();
                    case 2 -> listarProductosPorCategoria(scanner);
                    case 3 -> crearProducto(scanner);
                    case 4 -> editarProducto(scanner);
                    case 5 -> eliminarProducto(scanner);
                    case 0 -> volver = true;
                }
            } catch (NegocioException e) {
                System.out.println("\n[ERROR DE NEGOCIO] " + e.getMessage());
            }
        }
    }

    private static void listarProductos() {
        System.out.println("\n--- LISTADO GENERAL DE PRODUCTOS ---");
        List<Producto> lista = prodService.listar();
        if (lista.isEmpty()) {
            System.out.println("No hay productos cargados.");
        } else {
            lista.forEach(System.out::println);
        }
    }

    private static void listarProductosPorCategoria(Scanner scanner) {
        System.out.println("\n--- FILTRAR PRODUCTOS POR CATEGORÍA ---");
        listarCategorias();
        Long catId = readLong(scanner, "Ingrese el ID de la categoría: ", true);
        Categoria cat = catService.buscarPorId(catId); // Validates category exists

        System.out.println("\nProductos de la categoría: " + cat.getNombre());
        List<Producto> lista = prodService.listarPorCategoria(catId);
        if (lista.isEmpty()) {
            System.out.println("No hay productos cargados en esta categoría.");
        } else {
            lista.forEach(System.out::println);
        }
    }

    private static void crearProducto(Scanner scanner) {
        System.out.println("\n--- NUEVO PRODUCTO ---");
        String nombre = readString(scanner, "Nombre: ", true);
        String desc = readString(scanner, "Descripción: ", false);
        Double precio = readDouble(scanner, "Precio: ", true, 0.0);
        Integer stock = readInt(scanner, "Stock inicial: ", true, 0, null);
        String img = readString(scanner, "Ruta de Imagen (ej. pizza.png): ", false);

        listarCategorias();
        Long catId = readLong(scanner, "Asocie una categoría (ID): ", true);
        Categoria cat = catService.buscarPorId(catId);

        Producto nuevo = prodService.crear(nombre, precio, desc, stock, img, true, cat);
        System.out.println("\n[ÉXITO] Producto creado correctamente. ID asignado: " + nuevo.getId());
    }

    private static void editarProducto(Scanner scanner) {
        System.out.println("\n--- EDITAR PRODUCTO ---");
        listarProductos();
        Long id = readLong(scanner, "Ingrese el ID del producto a editar: ", true);
        Producto actual = prodService.buscarPorId(id);

        System.out.println("\nEditando producto: " + actual.getNombre());
        String nombre = readString(scanner, "Nuevo nombre (Enter para mantener [" + actual.getNombre() + "]): ", false);
        if (nombre.isEmpty()) {
            nombre = null;
        }

        Double precio = readDouble(scanner, "Nuevo precio (Enter para mantener [$" + actual.getPrecio() + "]): ", false, 0.0);

        String desc = readString(scanner, "Nueva descripción (Enter para mantener [" + actual.getDescripcion() + "]): ", false);
        if (desc.isEmpty()) {
            desc = null;
        }

        Integer stock = readInt(scanner, "Nuevo stock (Enter para mantener [" + actual.getStock() + "]): ", false, 0, null);

        String img = readString(scanner, "Nueva imagen (Enter para mantener [" + actual.getImagen() + "]): ", false);
        if (img.isEmpty()) {
            img = null;
        }

        System.out.println("\n¿Disponible? (Actual: " + (actual.getDisponible() ? "Sí" : "No") + ")");
        System.out.println("1. Disponible (Sí)");
        System.out.println("2. No Disponible (No)");
        System.out.println("3. Mantener actual");
        Integer dispOpt = readInt(scanner, "Seleccione opción: ", false, 1, 3);
        Boolean disponible = null;
        if (dispOpt != null && dispOpt != 3) {
            disponible = (dispOpt == 1);
        }

        System.out.println("\nCategoría (Actual: " + (actual.getCategoria() != null ? actual.getCategoria().getNombre() : "Ninguna") + ")");
        System.out.println("1. Cambiar categoría");
        System.out.println("2. Mantener actual");
        Integer catOpt = readInt(scanner, "Seleccione opción: ", false, 1, 2);
        Categoria categoria = null;
        if (catOpt != null && catOpt == 1) {
            listarCategorias();
            Long catId = readLong(scanner, "Ingrese el ID de la nueva categoría: ", true);
            categoria = catService.buscarPorId(catId);
        }

        prodService.editar(id, nombre, precio, desc, stock, img, disponible, categoria);
        System.out.println("\n[ÉXITO] Producto editado con éxito.");
    }

    private static void eliminarProducto(Scanner scanner) {
        System.out.println("\n--- ELIMINAR PRODUCTO ---");
        listarProductos();
        Long id = readLong(scanner, "Ingrese el ID del producto a eliminar: ", true);
        Producto actual = prodService.buscarPorId(id);

        System.out.println("\nProducto seleccionado: " + actual.getNombre());
        String confirm = readString(scanner, "¿Está seguro que desea eliminar el producto? (S/N): ", true);
        if (confirm.equalsIgnoreCase("s")) {
            prodService.eliminar(id);
            System.out.println("\n[ÉXITO] Producto eliminado correctamente (baja lógica).");
        } else {
            System.out.println("\nOperación cancelada.");
        }
    }

    // ==========================================
    // MENÚ USUARIOS
    // ==========================================
    private static void menuUsuarios(Scanner scanner) {
        boolean volver = false;
        while (!volver) {
            try {
                System.out.println("\n--- SUBMENÚ GESTIÓN DE USUARIOS ---");
                System.out.println("1. Listar usuarios");
                System.out.println("2. Crear usuario");
                System.out.println("3. Editar usuario");
                System.out.println("4. Eliminar usuario (baja lógica)");
                System.out.println("0. Volver");
                int opcion = readInt(scanner, "Seleccione una opción: ", true, 0, 4);

                switch (opcion) {
                    case 1 -> listarUsuarios();
                    case 2 -> crearUsuario(scanner);
                    case 3 -> editarUsuario(scanner);
                    case 4 -> eliminarUsuario(scanner);
                    case 0 -> volver = true;
                }
            } catch (NegocioException e) {
                System.out.println("\n[ERROR DE NEGOCIO] " + e.getMessage());
            }
        }
    }

    private static void listarUsuarios() {
        System.out.println("\n--- LISTADO DE USUARIOS ACTIVOS ---");
        List<Usuario> lista = usrService.listar();
        if (lista.isEmpty()) {
            System.out.println("No hay usuarios cargados.");
        } else {
            lista.forEach(System.out::println);
        }
    }

    private static void crearUsuario(Scanner scanner) {
        System.out.println("\n--- NUEVO USUARIO ---");
        String nombre = readString(scanner, "Nombre: ", true);
        String apellido = readString(scanner, "Apellido: ", true);
        String mail = readString(scanner, "Email (único): ", true);
        String celular = readString(scanner, "Celular: ", false);
        String contrasena = readString(scanner, "Contraseña: ", true);
        Rol rol = readRol(scanner, "Rol: ", true);

        Usuario nuevo = usrService.crear(nombre, apellido, mail, celular, contrasena, rol);
        System.out.println("\n[ÉXITO] Usuario creado correctamente. ID asignado: " + nuevo.getId());
    }

    private static void editarUsuario(Scanner scanner) {
        System.out.println("\n--- EDITAR USUARIO ---");
        listarUsuarios();
        Long id = readLong(scanner, "Ingrese el ID del usuario a editar: ", true);
        Usuario actual = usrService.buscarPorId(id);

        System.out.println("\nEditando usuario: " + actual.getNombre() + " " + actual.getApellido());
        String nombre = readString(scanner, "Nuevo nombre (Enter para mantener [" + actual.getNombre() + "]): ", false);
        if (nombre.isEmpty()) {
            nombre = null;
        }
        String apellido = readString(scanner, "Nuevo apellido (Enter para mantener [" + actual.getApellido() + "]): ", false);
        if (apellido.isEmpty()) {
            apellido = null;
        }
        String mail = readString(scanner, "Nuevo email (Enter para mantener [" + actual.getMail() + "]): ", false);
        if (mail.isEmpty()) {
            mail = null;
        }
        String celular = readString(scanner, "Nuevo celular (Enter para mantener [" + actual.getCelular() + "]): ", false);
        if (celular.isEmpty()) {
            celular = null;
        }
        String contrasena = readString(scanner, "Nueva contraseña (Enter para mantener): ", false);
        if (contrasena.isEmpty()) {
            contrasena = null;
        }
        Rol rol = readRol(scanner, "Seleccione nuevo rol (Enter para mantener [" + actual.getRol() + "]): ", false);

        usrService.editar(id, nombre, apellido, mail, celular, contrasena, rol);
        System.out.println("\n[ÉXITO] Usuario editado con éxito.");
    }

    private static void eliminarUsuario(Scanner scanner) {
        System.out.println("\n--- ELIMINAR USUARIO ---");
        listarUsuarios();
        Long id = readLong(scanner, "Ingrese el ID del usuario a eliminar: ", true);
        Usuario actual = usrService.buscarPorId(id);

        System.out.println("\nUsuario seleccionado: " + actual.getNombre() + " " + actual.getApellido());
        String confirm = readString(scanner, "¿Está seguro que desea eliminar el usuario? (S/N): ", true);
        if (confirm.equalsIgnoreCase("s")) {
            usrService.eliminar(id);
            System.out.println("\n[ÉXITO] Usuario eliminado correctamente (baja lógica).");
        } else {
            System.out.println("\nOperación cancelada.");
        }
    }

    // ==========================================
    // MENÚ PEDIDOS
    // ==========================================
    private static void menuPedidos(Scanner scanner) {
        boolean volver = false;
        while (!volver) {
            try {
                System.out.println("\n--- SUBMENÚ GESTIÓN DE PEDIDOS ---");
                System.out.println("1. Listar pedidos (general)");
                System.out.println("2. Listar pedidos por usuario");
                System.out.println("3. Crear pedido con detalles");
                System.out.println("4. Actualizar estado/forma de pago de pedido");
                System.out.println("5. Eliminar pedido (baja lógica)");
                System.out.println("0. Volver");
                int opcion = readInt(scanner, "Seleccione una opción: ", true, 0, 5);

                switch (opcion) {
                    case 1 -> listarPedidos();
                    case 2 -> listarPedidosPorUsuario(scanner);
                    case 3 -> crearPedido(scanner);
                    case 4 -> actualizarPedido(scanner);
                    case 5 -> eliminarPedido(scanner);
                    case 0 -> volver = true;
                }
            } catch (NegocioException e) {
                System.out.println("\n[ERROR DE NEGOCIO] " + e.getMessage());
            }
        }
    }

    private static void listarPedidos() {
        System.out.println("\n--- LISTADO GENERAL DE PEDIDOS ---");
        List<Pedido> lista = pedService.listar();
        if (lista.isEmpty()) {
            System.out.println("No hay pedidos registrados.");
        } else {
            lista.forEach(p -> System.out.println(p + "\n-------------------------------------------------"));
        }
    }

    private static void listarPedidosPorUsuario(Scanner scanner) {
        System.out.println("\n--- FILTRAR PEDIDOS POR USUARIO ---");
        listarUsuarios();
        Long usrId = readLong(scanner, "Ingrese el ID del usuario: ", true);
        Usuario usr = usrService.buscarPorId(usrId);

        System.out.println("\nPedidos del usuario: " + usr.getNombre() + " " + usr.getApellido());
        List<Pedido> lista = pedService.listarPorUsuario(usrId);
        if (lista.isEmpty()) {
            System.out.println("No hay pedidos registrados para este usuario.");
        } else {
            lista.forEach(p -> System.out.println(p + "\n-------------------------------------------------"));
        }
    }

    private static void crearPedido(Scanner scanner) {
        System.out.println("\n--- CREAR NUEVO PEDIDO ---");
        listarUsuarios();
        Long usrId = readLong(scanner, "Ingrese el ID del usuario que realiza el pedido: ", true);
        Usuario usr = usrService.buscarPorId(usrId);

        FormaPago formaPago = readFormaPago(scanner, "Seleccione la forma de pago: ", true);

        List<PedidoService.DetalleInput> detalles = new ArrayList<>();
        boolean agregarMas = true;

        while (agregarMas) {
            listarProductos();
            Long prodId = readLong(scanner, "Ingrese el ID del producto a agregar: ", true);
            Producto prod = prodService.buscarPorId(prodId);

            int cantidad = readInt(scanner, "Ingrese la cantidad: ", true, 1, null);
            detalles.add(new PedidoService.DetalleInput(prod, cantidad));

            String masOpt = readString(scanner, "¿Desea agregar otro producto al pedido? (S/N): ", true);
            if (!masOpt.equalsIgnoreCase("s")) {
                agregarMas = false;
            }
        }

        // Create the order in DB via transaction
        Pedido creado = pedService.crear(usr, formaPago, detalles);
        System.out.println("\n[ÉXITO] Pedido creado correctamente.");
        System.out.println(creado);
    }

    private static void actualizarPedido(Scanner scanner) {
        System.out.println("\n--- ACTUALIZAR ESTADO / FORMA DE PAGO ---");
        listarPedidos();
        Long id = readLong(scanner, "Ingrese el ID del pedido a actualizar: ", true);
        Pedido actual = pedService.buscarPorId(id);

        System.out.println("\n1. Cambiar Estado");
        System.out.println("2. Cambiar Forma de Pago");
        System.out.println("3. Cambiar Ambos");
        System.out.println("4. Cancelar");
        int opt = readInt(scanner, "Seleccione una opción: ", true, 1, 4);

        Estado nuevoEstado = null;
        FormaPago nuevaFormaPago = null;

        if (opt == 1 || opt == 3) {
            nuevoEstado = readEstado(scanner, "Seleccione el nuevo estado: ", true);
        }
        if (opt == 2 || opt == 3) {
            nuevaFormaPago = readFormaPago(scanner, "Seleccione la nueva forma de pago: ", true);
        }

        if (opt != 4 && (nuevoEstado != null || nuevaFormaPago != null)) {
            pedService.actualizarEstadoYFormaPago(id, nuevoEstado, nuevaFormaPago);
            System.out.println("\n[ÉXITO] Pedido actualizado correctamente.");
        } else {
            System.out.println("\nOperación cancelada.");
        }
    }

    private static void eliminarPedido(Scanner scanner) {
        System.out.println("\n--- ELIMINAR PEDIDO ---");
        listarPedidos();
        Long id = readLong(scanner, "Ingrese el ID del pedido a eliminar: ", true);
        Pedido actual = pedService.buscarPorId(id);

        System.out.println("\nPedido seleccionado:");
        System.out.println(actual);

        String confirm = readString(scanner, "¿Está seguro que desea eliminar este pedido (baja lógica)? (S/N): ", true);
        if (confirm.equalsIgnoreCase("s")) {
            pedService.eliminar(id);
            System.out.println("\n[ÉXITO] Pedido eliminado correctamente (baja lógica).");
        } else {
            System.out.println("\nOperación cancelada.");
        }
    }

    // ==========================================
    // ROBUST USER INPUT READING UTILITIES
    // ==========================================
    private static String readString(Scanner scanner, String prompt, boolean required) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (required && input.isEmpty()) {
                System.out.println("Este campo es obligatorio. Por favor, ingrese un valor.");
                continue;
            }
            return input;
        }
    }

    private static Integer readInt(Scanner scanner, String prompt, boolean required, Integer min, Integer max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                if (required) {
                    System.out.println("Este campo es obligatorio.");
                    continue;
                }
                return null;
            }
            try {
                int val = Integer.parseInt(input);
                if (min != null && val < min) {
                    System.out.println("El valor debe ser mayor o igual a " + min);
                    continue;
                }
                if (max != null && val > max) {
                    System.out.println("El valor debe ser menor o igual a " + max);
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Ingrese un número entero.");
            }
        }
    }

    private static Double readDouble(Scanner scanner, String prompt, boolean required, Double min) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                if (required) {
                    System.out.println("Este campo es obligatorio.");
                    continue;
                }
                return null;
            }
            try {
                double val = Double.parseDouble(input);
                if (min != null && val < min) {
                    System.out.println("El valor debe ser mayor o igual a " + min);
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Ingrese un número decimal.");
            }
        }
    }

    private static Long readLong(Scanner scanner, String prompt, boolean required) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                if (required) {
                    System.out.println("Este campo es obligatorio.");
                    continue;
                }
                return null;
            }
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Ingrese un ID numérico.");
            }
        }
    }

    private static Rol readRol(Scanner scanner, String prompt, boolean required) {
        System.out.println("Roles disponibles:");
        System.out.println("1. ADMIN");
        System.out.println("2. USUARIO");
        Integer opt = readInt(scanner, prompt, required, 1, 2);
        if (opt == null) return null;
        return opt == 1 ? Rol.ADMIN : Rol.USUARIO;
    }

    private static Estado readEstado(Scanner scanner, String prompt, boolean required) {
        System.out.println("Estados disponibles:");
        System.out.println("1. PENDIENTE");
        System.out.println("2. CONFIRMADO");
        System.out.println("3. TERMINADO");
        System.out.println("4. CANCELADO");
        Integer opt = readInt(scanner, prompt, required, 1, 4);
        if (opt == null) return null;
        return switch (opt) {
            case 1 -> Estado.PENDIENTE;
            case 2 -> Estado.CONFIRMADO;
            case 3 -> Estado.TERMINADO;
            case 4 -> Estado.CANCELADO;
            default -> null;
        };
    }

    private static FormaPago readFormaPago(Scanner scanner, String prompt, boolean required) {
        System.out.println("Formas de Pago disponibles:");
        System.out.println("1. TARJETA");
        System.out.println("2. TRANSFERENCIA");
        System.out.println("3. EFECTIVO");
        Integer opt = readInt(scanner, prompt, required, 1, 3);
        if (opt == null) return null;
        return switch (opt) {
            case 1 -> FormaPago.TARJETA;
            case 2 -> FormaPago.TRANSFERENCIA;
            case 3 -> FormaPago.EFECTIVO;
            default -> null;
        };
    }
}
