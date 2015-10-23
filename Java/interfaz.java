/*
Esta es una aplicacion de java... dentro de las funciones fundamentales
que cumple esta aplicacion se encuentran las siguientes:

1. Genera una interfaz grafica que contiene un boton dos
tablas que en un primer momento se encuentran vacias.
2. Una vez ejecutado el programa, se ejecuta la funcion conexionArduino(), que
establece la conexion por puerto serie a 9600 baudios
3. El boton "iniciar conexion" permite ejecutar un método llamado recibirDatos() el cual
entra en un loop que esta constantemente preguntando si se están recibiendo
datos por el puerto serie... si es asi lee los datos y los envía al método
mostrarDatos() y al metodo mostrarFoto().
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
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
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
    
    //Strings para definir la fecha de ingreso o salida de personas
    String meses [] = {"ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"};
    String dias [] = {"Domingo", "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado"};

    public interfaz() {
        //inicializa todos los componentes para la interfaz gráfica:
        initComponents();
        conexionArduino();
        //inicializa la variable timer:
        timer = new Thread (new ImplementoRunnable());
        timer.start();
        timer.suspend();
    }
    
    /*
    ESTE MÉTODO MUESTRA LOS DATOS DE LA PERSONA QUE TOMA DE LA BBDD 
    TOMANDO COMO INPUT EL CODIGO INTERNO QUE RECIBE COMO STRING
    */  
    
    void mostrarDatos(String codigo, char ta){
        Calendar obj = new GregorianCalendar();
        int segundo;
        int minuto;
        int hora;
        int dia_semana;
        int dia_mes;
        int mes;
        int año;
        segundo = obj.get(Calendar.SECOND);
        minuto = obj.get(Calendar.MINUTE);
        hora = obj.get(Calendar.HOUR_OF_DAY);
        dia_mes = obj.get(Calendar.DAY_OF_MONTH);
        dia_semana = obj.get(Calendar.DAY_OF_WEEK);
        mes = obj.get(Calendar.MONTH);
        año = obj.get(Calendar.YEAR);
        
        String fecha = dias[dia_semana - 1] + " " + dia_mes + " " + meses[mes] + " " + año;
        String shora;
        String smin;
        String sseg;
        if (hora < 10)
            shora = "0" + hora;
        else
            shora = "" + hora;
        if (minuto < 10)
            smin = "0" + minuto;
        else
            smin = "" + minuto;
        if (segundo < 10)
            sseg = "0" + segundo;
        else
            sseg = "" + segundo;
        String horario = shora + ":" + smin + ":" + sseg;
        
        
        PreparedStatement pst;
        try {
            if(ta == 'i'){
                pst = cn.prepareStatement("INSERT INTO ingreso (ingreso_fecha, ingreso_horario, persona_codigoint) VALUES ('" + fecha + "', '" + horario + "', '" + codigo + "')");
                pst.executeUpdate();
            }
            if(ta == 's'){
                pst = cn.prepareStatement("INSERT INTO salida (salida_fecha, salida_horario, persona_codigoint) VALUES ('" + fecha + "', '" + horario + "', '" + codigo + "')");
                pst.executeUpdate();                
            }
               
        } catch (SQLException ex) {
            Logger.getLogger(interfaz.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        DefaultTableModel modelo = new DefaultTableModel();
        DefaultTableModel modelo2 = new DefaultTableModel();
        modelo.addColumn("Cargo");
        modelo.addColumn("Apellido");
        modelo.addColumn("Nombre");
        if(ta == 'i')
            modelo.addColumn("Fecha de Ingreso");
        if(ta == 's')
            modelo.addColumn("Fecha de Salida");
        modelo.addColumn("Horario");
        modelo2.addColumn("Patente");
        modelo2.addColumn("Marca");
        modelo2.addColumn("Color");
        Tabla1.setModel(modelo);
        Tabla1.getColumnModel().getColumn(0).setPreferredWidth(2);
        Tabla1.getColumnModel().getColumn(4).setPreferredWidth(1);
        Tabla2.setModel(modelo2);
        String []datos = new String [5];
        try{
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM persona WHERE persona_codigoint = '"+codigo+"'");
            if(rs.next()){
                datos[0] = rs.getString(1);
                datos[1] = rs.getString(2);
                datos[2] = rs.getString(3);
                datos[3] = fecha;
                datos[4] = horario;
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
                    datos[3] = fecha;
                    datos[4] = horario;
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
            ResultSet rs = sta.executeQuery("SELECT A.* FROM auto A, persona_auto PA WHERE PA.id_auto = A.id_auto AND PA.persona_codigoint = '" + codigo + "'");
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
        Label_foto = new javax.swing.JLabel();
        Btn_comenzar_lectura = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        Tabla2 = new javax.swing.JTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        Tabla1 = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("InterfazGral");

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        Btn_comenzar_lectura.setText("Comenzar lectura");
        Btn_comenzar_lectura.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Btn_comenzar_lecturaActionPerformed(evt);
            }
        });

        Tabla2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        Tabla2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane2.setViewportView(Tabla2);

        Tabla1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        Tabla1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(Tabla1);

        jLabel1.setFont(new java.awt.Font("Times New Roman", 0, 24)); // NOI18N
        jLabel1.setText("Oprima \"Comenzar lectura\" y espere");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(Label_foto, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(Btn_comenzar_lectura, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 532, Short.MAX_VALUE)
                            .addComponent(jScrollPane2))
                        .addContainerGap(15, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(Btn_comenzar_lectura, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(Label_foto, javax.swing.GroupLayout.PREFERRED_SIZE, 337, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(111, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>                        

    private void Btn_comenzar_lecturaActionPerformed(java.awt.event.ActionEvent evt) {                                                     
        timer.resume();//AQUI PERMITE INICIALIZAR EL METODO implementoRunnable().. EL CUAL LLAMA
        //A LA FUNCION recibirDatos();
        Btn_comenzar_lectura.setEnabled(false);
        jLabel1.setText("Leyendo...");
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
    private javax.swing.JButton Btn_comenzar_lectura;
    private javax.swing.JLabel Label_foto;
    private javax.swing.JTable Tabla1;
    private javax.swing.JTable Tabla2;
    private javax.swing.JLabel jLabel1;
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
        //Definimos el tamaño de la foto con la siguiente linea. Mantener proporcion 320x240:
        Label_foto.setSize(450, 337);
        
        while(true){
            String codigo = "";
            char tipoAcceso = 0;
            
            try {
                if(input.available()!=0){
                    System.out.print("Nuevo ingreso: ");
                    cod = (char) input.read();
                    while(cod != 'z'){
                        try{
                            if(cod!=','){
                            codigo = codigo + cod;
                            cod = (char) input.read();
                            }
                            else
                            {
                                                                  
                                cod = (char) input.read();
                                tipoAcceso = cod;
                                cod = (char) input.read();
                            }
                            //JOptionPane.showMessageDialog(this, "Lo que recibo por puerto serie es: "+cod);
                            //System.out.println(cod);
                        } catch(Exception e){
                            mostrarError("Error en el metodo recibirDatos()");
                        }
                    }
                    cod = (char) input.read();
                    //cod = (char) input.read();
                    System.out.println(codigo);
                    mostrarFoto(codigo);
                    mostrarDatos(codigo, tipoAcceso);
                    
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

    public void mostrarFoto(String cod){
        //Nota: para cambiar el tamaño de la foto ir al metodo "recibirDatos()" en la segunda linea
        String path = "F:\\Documents\\Fedo\\Proyecto Automatizacion de Barrera\\fotos plana mayor\\" + cod + ".jpg";
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage();
        Image newimg = img.getScaledInstance(Label_foto.getWidth(), Label_foto.getHeight(), Image.SCALE_SMOOTH);
        ImageIcon image = new ImageIcon(newimg);
        Label_foto.setIcon(image);
    }
    
    private class ImplementoRunnable implements Runnable {
        public void run() {
            recibirDatos();
        }
    }
}
