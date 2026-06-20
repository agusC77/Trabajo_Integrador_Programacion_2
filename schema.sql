-- =====================================================================
-- Food Store - Base de Datos (Sistema de Gestión de Pedidos)
-- =====================================================================

-- Crear base de datos si no existe
CREATE DATABASE IF NOT EXISTS foodstore;
USE foodstore;

-- 1. Tabla: Categorías (Hereda de Base: id, eliminado, created_at)
CREATE TABLE IF NOT EXISTS categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_categorias_eliminado (eliminado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tabla: Productos (Hereda de Base + relación N:1 con Categorías)
CREATE TABLE IF NOT EXISTS productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    descripcion TEXT,
    stock INT NOT NULL,
    imagen VARCHAR(255),
    disponible BOOLEAN NOT NULL DEFAULT TRUE,
    categoria_id BIGINT NOT NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_producto_precio CHECK (precio >= 0),
    CONSTRAINT chk_producto_stock CHECK (stock >= 0),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    INDEX idx_productos_eliminado (eliminado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tabla: Usuarios (Hereda de Base)
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    mail VARCHAR(150) NOT NULL UNIQUE,
    celular VARCHAR(50),
    contrasena VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN', 'USUARIO')),
    INDEX idx_usuarios_eliminado (eliminado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tabla: Pedidos (Hereda de Base + relación N:1 con Usuarios)
CREATE TABLE IF NOT EXISTS pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    estado VARCHAR(20) NOT NULL,
    total DECIMAL(10, 2) NOT NULL DEFAULT 0.0,
    forma_pago VARCHAR(20) NOT NULL,
    usuario_id BIGINT NOT NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pedido_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADO', 'TERMINADO', 'CANCELADO')),
    CONSTRAINT chk_pedido_forma_pago CHECK (forma_pago IN ('TARJETA', 'TRANSFERENCIA', 'EFECTIVO')),
    CONSTRAINT fk_pedido_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    INDEX idx_pedidos_eliminado (eliminado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Tabla: DetallePedidos (Hereda de Base + composición 1:N con Pedidos + relación N:1 con Productos)
CREATE TABLE IF NOT EXISTS detalle_pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cantidad INT NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    producto_id BIGINT NOT NULL,
    pedido_id BIGINT NOT NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    INDEX idx_detalles_eliminado (eliminado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- INSERCIÓN DE DATOS DE PRUEBA (MOCK DATA)
-- =====================================================================

-- 1. Insertar Categorías
INSERT INTO categorias (id, nombre, descripcion) VALUES
(1, 'Pizzas', 'Pizzas caseras al horno de barro'),
(2, 'Hamburguesas', 'Hamburguesas gourmet con papas fritas'),
(3, 'Bebidas', 'Bebidas frías y gaseosas');

-- 2. Insertar Productos
INSERT INTO productos (id, nombre, precio, descripcion, stock, imagen, disponible, categoria_id) VALUES
(1, 'Pizza Muzzarella', 1200.00, 'Salsa de tomate, muzzarella y aceitunas', 15, 'muzza.jpg', TRUE, 1),
(2, 'Pizza Especial', 1500.00, 'Muzzarella, jamón cocido, morrones y aceitunas', 10, 'especial.jpg', TRUE, 1),
(3, 'Doble Cheddar', 1800.00, 'Doble carne, doble cheddar, bacon y aderezos', 8, 'cheddar.jpg', TRUE, 2),
(4, 'Coca Cola 500ml', 500.00, 'Gaseosa sabor original', 30, 'coca.jpg', TRUE, 3);

-- 3. Insertar Usuarios
INSERT INTO usuarios (id, nombre, apellido, mail, celular, contrasena, rol) VALUES
(1, 'Juan', 'Pérez', 'juan@foodstore.com', '11223344', 'admin123', 'ADMIN'),
(2, 'María', 'Gómez', 'maria@gmail.com', '55667788', 'maria123', 'USUARIO');

-- 4. Insertar Pedido de Muestra
INSERT INTO pedidos (id, fecha, estado, total, forma_pago, usuario_id) VALUES
(1, CURRENT_DATE(), 'PENDIENTE', 3900.00, 'EFECTIVO', 2);

-- 5. Insertar Detalles de Pedido de Muestra
INSERT INTO detalle_pedidos (id, cantidad, subtotal, producto_id, pedido_id) VALUES
(1, 2, 2400.00, 1, 1), -- 2x Pizza Muzzarella ($1200 c/u)
(2, 3, 1500.00, 4, 1); -- 3x Coca Cola ($500 c/u)
