# Food Store - Gestión de Pedidos (JDBC)

Este es el proyecto integrador desarrollado para la materia **Programación 2**. Es un sistema de gestión de pedidos de comida por consola escrito en Java 21 y conectado a una base de datos MySQL mediante JDBC.

### Autores:
* Luca Argumedo
* Enzo Giaquinta
* Agustin Contardi
* Pablo Allende

---

## Estructura del Proyecto

```text
Trabajo Integrador /
├── lib /                       # Librería de dependencias del proyecto (contiene el Driver JDBC)
├── src /
│   └── integrado /
│       └── prog2 /
│           ├── config /
│           │   └── ConexionDB.java        # Clase de conexión a la base de datos
│           ├── entities /                 # Clases del modelo UML
│           │   ├── Base.java
│           │   ├── Categoria.java
│           │   ├── DetallePedido.java
│           │   ├── Pedido.java
│           │   ├── Producto.java
│           │   └── Usuario.java
│           ├── enums /                    # Enumeraciones de estado, roles y pagos
│           │   ├── Estado.java
│           │   ├── FormaPago.java
│           │   └── Rol.java
│           ├── exception /                # Excepciones personalizadas
│           │   ├── EntidadNoEncontradaException.java
│           │   ├── NegocioException.java
│           │   ├── ReglaNegocioException.java
│           │   └── StockInvalidoException.java
│           ├── interfaces /               # Interfaz Calculable implementada en Pedido
│           │   └── Calculable.java
│           ├── service /                  # Lógica del sistema y consultas SQL
│           │   ├── CategoriaService.java
│           │   ├── PedidoService.java
│           │   ├── ProductoService.java
│           │   └── UsuarioService.java
│           ├── scratch /                  # Pruebas automatizadas de reglas de negocio
│           │   └── TestBusinessRules.java
│           └── Main.java                  # Interfaz de consola que interactúa con el usuario
├── schema.sql                  # Script SQL para levantar la base de datos foodstore
└── README.md                   # Instrucciones del proyecto
```

---

## Cómo levantar el proyecto

### 1. Configurar la Base de Datos
1. Inicia tu servidor local de MySQL (por ejemplo, con XAMPP o MySQL Workbench).
2. Importa el archivo `schema.sql` en tu servidor. Esto creará la base de datos llamada `foodstore`, las tablas necesarias y cargará algunos datos de prueba para que la app no empiece vacía.
3. Si usas usuario o contraseña distintas a las por defecto (`root` y sin contraseña), puedes editarlas en:
   `src/integrado/prog2/config/ConexionDB.java`

### 2. Importar el proyecto en un IDE (Opcional)
Si vas a trabajar desde un Entorno de Desarrollo Integrado, debes asegurarte de que el IDE reconozca el driver JDBC alojado en la carpeta lib:
- IntelliJ IDEA: Ve a File > Project Structure > Libraries, haz clic en el botón +, selecciona Java y busca el archivo .jar dentro de la carpeta lib.
- Eclipse: Haz clic derecho sobre el proyecto, ve a Build Path > Configure Build Path, en la pestaña Libraries selecciona Classpath, haz clic en Add JARs y selecciona el archivo de la carpeta lib.

### 3. Compilar el Código

Ubicarse en la carpeta raíz del proyecto (`Trabajo Integrador`).

*(Usamos `-encoding UTF-8` para evitar problemas con caracteres especiales como la ñ o acentos en Windows).*

#### Windows (PowerShell)

```powershell
javac -encoding UTF-8 -d bin src\integrado\prog2\enums\*.java src\integrado\prog2\interfaces\*.java src\integrado\prog2\exception\*.java src\integrado\prog2\entities\*.java src\integrado\prog2\service\*.java src\integrado\prog2\config\*.java src\integrado\prog2\Main.java
```

#### Linux / macOS

```bash
javac -encoding UTF-8 -d bin src/integrado/prog2/enums/*.java src/integrado/prog2/interfaces/*.java src/integrado/prog2/exception/*.java src/integrado/prog2/entities/*.java src/integrado/prog2/service/*.java src/integrado/prog2/config/*.java src/integrado/prog2/Main.java
```

### 4. Ejecutar la Aplicación
Para iniciar el sistema de consola:

* **En Windows (PowerShell):**
  ```bash
  java -cp "bin;lib/*" integrado.prog2.Main
  ```
* **En Linux / macOS:**
  ```bash
  java -cp "bin:lib/*" integrado.prog2.Main
  ```

---

## Ejecutar las Pruebas de Reglas de Negocio
Si quieres correr el programa de verificación automatizada que testea que las reglas de negocio (validación de stock, transacciones, unicidad de correos, etc.) funcionen correctamente contra la base de datos:

1. Compila la clase de test:
   ```bash
   javac -encoding UTF-8 -d bin -cp bin src/integrado/prog2/scratch/TestBusinessRules.java
   ```
2. Ejecuta el test:
   ```bash
   java -cp "bin;lib/*" integrado.prog2.TestBusinessRules
   ```
