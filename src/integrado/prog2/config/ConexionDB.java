package integrado.prog2.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {
    private static final String URL = "jdbc:mysql://localhost:3306/foodstore?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Por defecto vacío, se puede cambiar si el usuario tiene contraseña

    /**
     * Obtiene una conexión activa a la base de datos MySQL.
     * Carga el driver por reflexión para asegurar compatibilidad.
     *
     * @return Connection objeto de conexión JDBC activo
     * @throws SQLException si ocurre un error en la conexión o no se encuentra el driver
     */
    public static Connection obtenerConexion() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC de MySQL no encontrado en el classpath. Asegúrese de agregar mysql-connector-j.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
