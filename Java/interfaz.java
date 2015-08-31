/*
Esta es una aplicación de java... necesita si o si de la clase conectar.java para
realizar la conexion con la base de datos..
Tambien debe ser guardada como interfaz.java y debe ser guardada en la misma
carpeta en la que esta la clase conectar.java
Dentro de las funciones fundamentales
que cumple esta aplicación se encuentran las siguientes:

1. Genera una interfaz gráfica que contiene un txtField, tres botones y dos
tablas que en un primer momento se encuentran vacías.
2. Uno de los botones ejecuta un método llamado conexionArduino() el cual
abre el puerto Serie definido por el programador, y establece una conexión
de 9600 baudios para la recepción y envío de tramas.
3. Uno de los botones permite ejecutar un método llamado recibirDatos() el cual
entra en un loop que esta constantemente preguntando si se están recibiendo
datos por el puerto serie... si es asi lee los datos y los envía al método
mostrarDatos()
4. El método mostrarDatos() recibe el String formado en el método recibirDatos()
el cual toma como base para hacer una consulta a la base de datos previamente
generada. La consulta pide seleccionar a todos los registros que tengan como
primary key el código recibido como String del método recibirDatos(). Si la
lista que recibe como resultado de la consulta no es vacía.. genera una tabla
en la interfaz gráfica mostrando todos los registros que cumplen con la
condición enviada. Esta primer tabla muestra los datos de la persona que
ingresó el código interno (desde Arduino mediante el lector RFID montado sobre
el mismo). Mientras que la segunda tabla se genera sólo si están archivados los
datos del auto de la persona. Además de generar estas dos tablas envía un pulso
('1') al Arduino confirmando que el registro se encuentra en la base de datos
para que el Arduino abra la barrera, mediante el método enviarDatos().
Si la respuesta a la consulta devuelve una lista vacía envía un '0' (cero) al
Arduino para que no abra la barrera.
5. El txtField se usa como prueba de que la conexión a la base de datos se
realizó con éxito. Para dicha conexión se encuentra la clase dentro del mismo
proyecto llamada conectar.java. Dentro del txtField se ingresa el código
perteneciente a cualquier registro dentro de la tabla "persona" de la base de
datos previamente generada.
*/
package arduino;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;


public final class interfaz extends javax.swing.JFrame {
    
    /*
    LOS SIGUIENTES SON PULSOS QUE ENVIA EL RASPBERRY AL ARDUINO..
    SI EL REGISTRO SE ENCUENTRA EN LA BASE DATOS ENVIA UN UNO.. SINO
    SE ENCUENTRA ENVIA UN CERO.. el 1 en la tabla ascii es 49...
    el cero es 48.
    */
    
    private static final int Abrir_Barrera = 49;
    private static final int Cerrar_Barrera = 48;
    
    /*
    LA SIGUIENTE VARIABLE ES MUY IMPORTANTE.. ES LA QUE VA A PERMITIR QUE
    LUEGO DE HACER CLICK EN EL BOTON "RECIBIR DATOS" EL CODIGO ENTRE 
    EN UN LOOP INTERMINABLE QUE VA A RECIBIR TODOS LOS DATOS QUE "ESCUCHE" POR 
    EL PUERTO SERIE..
    */
    Thread timer;
    
    //LOS SIGUIENTES SON PARA EL METODO CONEXIONARDUINO() (SON 5)
    private OutputStream output = null;
    private InputStream input = null;
    SerialPort serialPort;
    private final String PUERTO = "COM10"; //acá debo poner el nombre del puerto al cual está conectado el arduino.. en el raspberry el nombre cambia..

    private static final int TIMEOUT = 2000; //En milisegundos..
    private static final int DATA_RATE = 9600;//baudios de inicialización del puerto serie..
    
    
    public interfaz() {
        //inicializa todos los componentes para la interfaz gráfica:
        initComponents();
        //inicializa la variable timer:
        timer = new Thread (new ImplementoRunnable());
        timer.start();
        timer.suspend();
    }
    
    
    /*
    ESTE MÉTODO MUESTRA LOS DATOS DE LA PERSONA QUE TOMA DE LA BBDD 
    TOMANDO COMO INPUT EL CODIGO INTERNO QUE RECIBE COMO STRING
    */
    void mostrarDatos(String codigo){
        DefaultTableModel modelo = new DefaultTableModel();
        DefaultTableModel modelo2 = new DefaultTableModel();
        modelo.addColumn("Apellido");
        modelo.addColumn("Nombre");
        modelo.addColumn("Documento");
        modelo2.addColumn("Patente");
        modelo2.addColumn("Marca");
        modelo2.addColumn("Color");
        Tabla1.setModel(modelo);
        Tabla2.setModel(modelo2);
        String []datos = new String [3];
        try{
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM persona WHERE persona_codigoint = '"+codigo+"'");
            if(rs.next()){
                datos[0] = rs.getString(2);
                datos[1] = rs.getString(3);
                datos[2] = rs.getString(4);
                modelo.addRow(datos);
                
                /*
                SI LA TABLA QUE GENERA NO ESTA VACIA SIGNIFICA QUE SI HAY
                REGISTROS CON ESE CODIGO INTERNO RECIBIDO. ENTONCES ENVIA
                EL PULSO PARA ABRIR LA BARRERA
                */
                enviarDatos(Abrir_Barrera);
                while(rs.next()){
                    datos[0] = rs.getString(2);
                    datos[1] = rs.getString(3);
                    datos[2] = rs.getString(4);
                    modelo.addRow(datos);
                }
            } else{
                JOptionPane.showMessageDialog(this, "No se encontraron registros con el código ingresado");
                
                /*
                COMO NO SE ENCONTRARON REGISTROS CON EL CODIGO INTERNO
                RECIBIDO.. MANDA UN CERO PARA NO ABRIR LA BARRERA
                */
                enviarDatos(Cerrar_Barrera);
                //System.out.println("no hay registros con ese codigo");
            }
            Tabla1.setModel(modelo);
        } catch (SQLException ex){
            Logger.getLogger(interfaz.class.getName()).log(Level.SEVERE, null, ex);
        }
        String []datos2 = new String [3];
        try{
            Statement sta = cn.createStatement();
            ResultSet rs = sta.executeQuery("SELECT A.* FROM auto A, persona_auto PA WHERE PA.id_auto = A.id_auto AND PA.id_persona = (SELECT id_persona FROM persona WHERE persona_codigoint = '"+codigo+"')");
            while(rs.next()){
                datos2[0] = rs.getString(2);
                datos2[1] = rs.getString(3);
                datos2[2] = rs.getString(4);
                modelo2.addRow(datos2);
            }
            Tabla2.setModel(modelo2);
        } catch(SQLException e){
            Logger.getLogger(interfaz.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        TxtField1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        Tabla1 = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        Tabla2 = new javax.swing.JTable();
        campo_codigoint = new javax.swing.JTextField();
        etiqueta2 = new javax.swing.JLabel();
        Btn_buscar = new javax.swing.JButton();
        Btn_conectar_pto_serie = new javax.swing.JButton();
        Btn_recibir_datos = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("InterfazGral");

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        TxtField1.setText("Esperando recibir datos...");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(TxtField1)
                .addGap(247, 247, 247))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(TxtField1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        Tabla1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(Tabla1);

        Tabla2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane2.setViewportView(Tabla2);

        etiqueta2.setText("Ingrese un codigo interno:");

        Btn_buscar.setText("Buscar");
        Btn_buscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Btn_buscarActionPerformed(evt);
            }
        });

        Btn_conectar_pto_serie.setText("Conectar a Puerto Serie");
        Btn_conectar_pto_serie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Btn_conectar_pto_serieActionPerformed(evt);
            }
        });

        Btn_recibir_datos.setText("Recibir Datos");
        Btn_recibir_datos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Btn_recibir_datosActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(campo_codigoint, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(etiqueta2)
                    .addComponent(Btn_buscar)
                    .addComponent(Btn_conectar_pto_serie)
                    .addComponent(Btn_recibir_datos))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2))
                .addContainerGap())
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(etiqueta2)
                        .addGap(11, 11, 11)
                        .addComponent(campo_codigoint, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(Btn_buscar)
                        .addGap(18, 18, 18)
                        .addComponent(Btn_conectar_pto_serie)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(Btn_recibir_datos)))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>                        

    private void Btn_buscarActionPerformed(java.awt.event.ActionEvent evt) {                                           
        // TODO add your handling code here:
        mostrarDatos(campo_codigoint.getText());
    }                                          

    private void Btn_conectar_pto_serieActionPerformed(java.awt.event.ActionEvent evt) {                                                       
        conexionArduino();
    }                                                      

    private void Btn_recibir_datosActionPerformed(java.awt.event.ActionEvent evt) {                                                  
        timer.resume();//AQUI PERMITE INICIALIZAR EL METODO implementoRunnable().. EL CUAL LLAMA
        //A LA FUNCION recibirDatos();
    }                                                 

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new interfaz().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify                     
    private javax.swing.JButton Btn_buscar;
    private javax.swing.JButton Btn_conectar_pto_serie;
    private javax.swing.JButton Btn_recibir_datos;
    private javax.swing.JTable Tabla1;
    private javax.swing.JTable Tabla2;
    private javax.swing.JLabel TxtField1;
    private javax.swing.JTextField campo_codigoint;
    private javax.swing.JLabel etiqueta2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    // End of variables declaration                   

//Las siguientes son variables necesarias para establecer la conexión a la base
//de datos
conectar cc = new conectar();
Connection cn = cc.conexion();

    private void conexionArduino() {
        CommPortIdentifier puertoID = null;
        Enumeration puertoEnum = CommPortIdentifier.getPortIdentifiers();
        while(puertoEnum.hasMoreElements()){//busca puerto por puerto hasta encontrar el mismo puerto al cual está conectado el arduino
            CommPortIdentifier actualPuertoID = (CommPortIdentifier) puertoEnum.nextElement();
            if(PUERTO.equals(actualPuertoID.getName())){
                puertoID = actualPuertoID;
                break;
            }
        }
        if(puertoID == null){
            mostrarError("No se puede conectar al puerto (metodo conexionArduino)");
            System.exit(ERROR);
        }
        
        try{
            serialPort = (SerialPort) puertoID.open(this.getClass().getName(), TIMEOUT);
            //ahora configuramos los parametros del puerto serie
            serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            output = serialPort.getOutputStream();
            input = serialPort.getInputStream();
        } catch(Exception e){
            //mostrarError(e.getMessage());
            mostrarError("El puerto se encuentra en uso.. cerrar el monitor serial del IDE Arduino");
            System.exit(ERROR);
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(interfaz.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void mostrarError(String mensaje){
	JOptionPane.showMessageDialog(this, mensaje, "ERROR", JOptionPane.ERROR_MESSAGE);
    }
    
    public void enviarDatos(int pulso){
        try{
            output.write(pulso);
            //JOptionPane.showMessageDialog(this, "El pulso que envía es: "+pulso);
        } catch(Exception e){
            mostrarError("Error en el metodo enviarDatos");
            System.exit(ERROR);
        }
    }
    
    public void recibirDatos(){
        char cod;
        
        while(true){
            String codigo = "";
            try {
                if(input.available()!=0){
                    System.out.print("Nuevo ingreso: ");
                    for(int i=0; i<=11; i++){
                        try{
                            //if(i==0) Thread.sleep(2000);
                            cod = (char) input.read();
                            codigo = codigo + cod;
                            //JOptionPane.showMessageDialog(this, "Lo que recibo por puerto serie es: "+cod);
                            //System.out.println(cod);
                        } catch(Exception e){
                            mostrarError("Error en el metodo recibirDatos()");
                        }
                    }
                    System.out.println(codigo);
                    mostrarDatos(codigo);
                    //try {
                    //    Thread.sleep(2000);
                    //} catch (InterruptedException ex) {
                    //    Logger.getLogger(interfaz.class.getName()).log(Level.SEVERE, null, ex);
                    //}
                }
            } catch (IOException ex) {
                Logger.getLogger(interfaz.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class ImplementoRunnable implements Runnable {
        public void run() {
            recibirDatos();
        }
    }
}
