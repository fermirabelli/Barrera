En esta carpeta se encuentran los códigos de la aplicación java para poder ejecutar en una pc Windows.. para poder correrla en un raspberry hay que tener en cuenta lo siguiente:

Al comienzo del código en java se define el puerto serie por el que establecerá la conexión... en Windows este puerto se llama "COM10".. en Raspbian (SO utilizado en raspberry) el puerto serie por defecto cuando se conecta el arduino al rasberry por USB se llama "/dev/ttyACM0" por defecto, y si la conexión se realiza pin a pin rx tx, el puerto por defecto se llama "/dev/ttyAMA0"..
Sin embargo Linux tiene problemas para procesar dichos puertos así como están con ese nombre.. con lo cual habrá que establecer un enlace de un "nombre de un puerto serie inventado" al puerto en cuestión.. para esto en el terminal de raspberry ejecutamos lo siguiente (si se trata de una conexión pin a pin):

sudo ln -s /dev/ttyAMA0 /dev/ttyS80

'S80' vendría a ser el enlace que apunta a /dev/ttyAMA0.
Si se trata de una conexión vía USB:

sudo ln -s /dev/ttyACM0 /dev/ttyS80

Y además se deberá modificar el nombre del puerto serie en el codigo de java a "/dev/ttyS80".
nota: el nombre /dev/ttyS80 pudo haber sido /dev/ttyS81 o /dev/ttyS82 o cualquier numero grande que sepamos que no va a ser utilizado... podemos ver la lista de puertos disponibles en raspberry con el comando (desde el terminal) "ls /dev/tty*".
