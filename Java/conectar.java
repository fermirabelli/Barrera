/*
Este codigo genera la conexion entre la interfaz creada en java y la base de datos de mysql.
Si o si debe ser guardada con el nombre conectar.java y guardada en la misma carpeta que la
clase interfaz.java.
El motor de base de datos que se utilizó específicamente para este proyecto fue phpmyadmin
*/
package arduino;

import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

public class conectar 
{
    Connection conectar = null;
    public Connection conexion()
    {
        try 
        {
            Class.forName("com.mysql.jdbc.Driver");
            conectar=DriverManager.getConnection("jdbc:mysql://localhost/mecatronica","root","");
        } catch(Exception e)
        {
            JOptionPane.showMessageDialog(null, e);
        }    
        return conectar;
    }
}
