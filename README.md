# Food Store - Sistema de Gestión de Pedidos de Comida (JDBC)

Este es el proyecto integrador desarrollado para la materia **Programación 2** de la Tecnicatura Universitaria en Programación. Se trata de un sistema de gestión de pedidos de comida por consola escrito en **Java 21**, conectado a una base de datos relacional **MySQL** a través de **JDBC**, aplicando Programación Orientada a Objetos (POO), interfaces y excepciones propias.

---

## 🛠️ Configuración de la Base de Datos

### Paso 1: Levantar el servidor MySQL
Asegúrate de que tu servidor local de base de datos MySQL esté activo (mediante XAMPP, WampServer o como servicio local en el puerto `3306`).

### Paso 2: Ejecutar el script SQL
Importa el archivo [schema.sql](file:///c:/Users/lucaa/Desktop/Trabajo%20Integrador/schema.sql) en tu servidor MySQL. Esto creará la base de datos `foodstore`, las tablas necesarias (con restricciones de claves y triggers) y cargará los datos iniciales de prueba (categorías, productos, usuarios y un pedido de muestra).

Puedes hacerlo desde phpMyAdmin o desde la consola con el comando:
```bash
mysql -u root -p < schema.sql
```
*(Si no tienes contraseña en tu base de datos local, simplemente omite `-p`)*

### Paso 3: Configurar credenciales (Opcional)
Si tu MySQL utiliza una contraseña o puerto diferente, puedes modificar las credenciales en el archivo [ConexionDB.java](file:///c:/Users/lucaa/Desktop/Trabajo%20Integrador/src/integrado/prog2/config/ConexionDB.java):
* `URL`: `jdbc:mysql://localhost:3306/foodstore`
* `USER`: `root`
* `PASSWORD`: `""`

---

## 🚀 Compilación y Ejecución

### Requisitos Previos
* **Java Development Kit (JDK) 21** o superior instalado.
* **MySQL Connector/J** (el driver JDBC de MySQL). Puedes descargarlo de la página oficial de Oracle o de Maven Central (archivo `.jar`), y colocarlo dentro de una carpeta llamada `lib` en la raíz del proyecto.

### Paso 1: Compilar el Proyecto
Dado que el código fuente utiliza caracteres especiales del idioma español (como la `ñ` y vocales con acento), debes compilar especificando la codificación UTF-8:

```bash
javac -encoding UTF-8 -d bin src/integrado/prog2/enums/*.java src/integrado/prog2/interfaces/*.java src/integrado/prog2/exception/*.java src/integrado/prog2/entities/*.java src/integrado/prog2/service/*.java src/integrado/prog2/config/*.java src/integrado/prog2/Main.java
```

### Paso 2: Ejecutar la Aplicación
Para ejecutar la aplicación por consola incluyendo el driver JDBC de MySQL colocado en la carpeta `lib`:

**En Windows (PowerShell / CMD):**
```powershell
java -cp "bin;lib/*" integrado.prog2.Main
```

**En Linux / macOS:**
```bash
java -cp "bin:lib/*" integrado.prog2.Main
```

---

## 🧪 Ejecución de Pruebas de Reglas de Negocio
Para correr la suite de verificación automatizada que testea todas las reglas de negocio directamente contra la base de datos MySQL (por ejemplo, transacciones de stock, unicidad de emails, borrado lógico, etc.):

1. Compila el archivo de prueba:
   ```bash
   javac -encoding UTF-8 -d bin -cp bin src/integrado/prog2/scratch/TestBusinessRules.java
   ```
2. Ejecuta el test suite:
   ```bash
   java -cp "bin;lib/*" integrado.prog2.TestBusinessRules
   ```

---

## 📁 Estructura del Proyecto Migrado a JDBC

El código fuente está estructurado de forma modular según las capas recomendadas:

```text
src/
└── integrado/prog2/
    ├── Main.java              # Vista / Menú de consola interactivo (Verifica conexión al iniciar)
    ├── config/
    │   └── ConexionDB.java    # Gestor de conexiones JDBC a MySQL (BD: foodstore)
    ├── entities/              # Clases de dominio (Base, Categoria, Producto, Usuario, Pedido, DetallePedido)
    ├── enums/                 # Enumeraciones (Rol, Estado, FormaPago)
    ├── exception/             # Excepciones propias del negocio
    ├── interfaces/            # Interfaces de comportamiento común (Calculable)
    ├── service/               # Servicios que realizan operaciones CRUD mediante sentencias SQL (JDBC)
    └── scratch/
        └── TestBusinessRules.java # Suite de tests que valida las reglas de negocio sobre la base de datos
```

---

## 🛡️ Detalles de Lógica y Transacciones Implementadas
* **Transacciones en Pedidos**: La creación de pedidos utiliza transacciones SQL (`setAutoCommit(false)`). Si algún producto no tiene stock, se lanza una excepción y la base de datos realiza un `rollback` de forma automática, garantizando que el pedido no se registre y el stock de otros productos no se altere.
* **Control de Stock dinámico**: Si un pedido cambia su estado a `CANCELADO` o se elimina lógicamente del sistema, el stock correspondiente se devuelve a cada producto en la base de datos automáticamente.
* **Restricción de Categorías**: No se puede eliminar una categoría si la base de datos detecta productos activos asociados a ella.
