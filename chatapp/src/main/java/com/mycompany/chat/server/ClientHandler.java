package com.mycompany.chat.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;

//Implementamos Runnable para poder sobrescribir el método run()
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server servidor;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private String usuario;

    /*El Socket solo sabe mover bytes, números sueltos sin significado. Tú quieres leer y escribir líneas de texto. 
    Cada clase se encarga de un solo salto, y se encadenan. 
    Arriba va la cadena de lectura y abajo la de escritura. El dibujo sigue a esta frase. */
    public ClientHandler(Socket socket,Server servidor) throws IOException{
        this.socket=socket;
        this.servidor=servidor;
        /*socket.getInputStream(): la tubería de entrada. Por ella llegan bytes crudos, sin ningún significado todavía.
        new InputStreamReader(..., UTF_8): el traductor de bytes a caracteres. 
        El UTF_8 le dice cómo traducir. Una letra como la ñ ocupa dos bytes, y sin esto dependería del ordenador que se vea bien o no.
        new BufferedReader(...): guarda lo que llega en un búfer y te da readLine(), que devuelve una línea entera hasta el salto de línea. 
        Sin él tendrías que leer letra a letra y detectar tú los saltos de línea. */
        this.reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
        this.writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        /*
        new PrintWriter(...): Crea un escritor de texto orientado a caracteres con métodos útiles de formato y escritura ( println).
        socket.getOutputStream(): Obtiene la corriente de salida cruda del Socket. Es el canal por donde salen los bytes hacia el otro extremo de la conexión.
        true (autoFlush): Activa el vaciado automático del búfer en cada llamada a println, printf o format (con %n). Si pusieras false, el texto podría quedarse retenido en memoria esperando a llenar el búfer o a que llamaras a writer.flush().
        StandardCharsets.UTF_8: Define la codificación de caracteres. Asegura que tildes, ñ, emojis u otros símbolos se conviertan a bytes siempre con UTF-8, sin depender de la configuración por defecto del sistema operativo. */
    }

    //run() es lo que hará el hilo independiente de este cliente mientras esté conectado.
    @Override
    public void run() {
        //try-with-next , try(...) se conoce como encabezado de recursos apuntando a los atributos de la clase
        try (socket; reader; writer) {
            String nick = reader.readLine();// 1ª línea = nickname (nuevo mini-protocolo)
            if (nick == null || nick.isBlank()) {//nick.isBlank() comrpueba si string is ' '
                return; // FIN
            }
            usuario = nick.trim();//trim() elimina los espacios en blanco al principio y al final de una cadena de texto 
 
            servidor.broadcast(usuario + " se unio");
            

            String linea;
            //Aqui se para la ejecución del metodo y la clase por .readLine() espera a recibir contenido para linea que serían los mensajes del cliente
            while ((linea = reader.readLine()) != null) {   // Fase 1 leía UNA línea
                servidor.broadcast(usuario + ": " + linea);
            }
        } catch (IOException e) {
            // Salta si la conexion se rompe de golpe (por ejemplo, cierran la terminal a lo bruto).
            System.err.println("Error con " + usuario + ": " + e.getMessage());
        } finally {
            servidor.remover(this);
 
            // Despues se avisa a los demas, pero solo si llego a presentarse.
            if (usuario != null) {
                servidor.broadcast(usuario + " se fue de la conversacion");
            }
        }
    }

    void enviar(String mensaje) {
        synchronized (writer) {            // dos hilos pueden escribir al MISMO writerd
            writer.println(mensaje);       // a la vez → se evita entrelazar mensajes
        }
    }
    /*
    Tener claro esto 2 cosas :
    -Cada cliente tiene su propio hilo co un writer el encargado de llevar al servidor
    -Un hilo puede llamar a un método de un objeto que pertenece a otro cliente
    
    Explicacion como funcina:
    1ª Entender:
    Imagina un Server un caable unido con cada cliente 
    
    Entrada al servidor (reader):
    Ana escribe "Hola" en su programa cliente.
    El dato viaja por la red desde el cliente hacia el servidor.
    En el ClientHandler de Ana, reader (BufferedReader) recibe/lee ese mensaje que viene del cliente.

    Salida desde el servidor (writer):
    El servidor quiere que a Luis le llegue "Ana: Hola" en su pantalla.
    El dato tiene que viajar desde el servidor hacia el cliente de Luis.
    En el ClientHandler de Luis, writer (PrintWriter) envía/escribe ese mensaje hacia el socket de Luis para que cruce la red hasta el programa de Luis.
    
    2º Enteder:
    Cada cliente tiene su propio hilo ejecutando run()
    Cuando entran Ana y Luis al chat, el servidor crea dos ClientHandler y dos hilos separados:
        Hilo de Ana: está dentro del while ((linea = reader.readLine()) != null) de ClientHandler de Ana, esperando que Ana escriba algo.
        Hilo de Luis: está dentro del while ((linea = reader.readLine()) != null) de ClientHandler de Luis, esperando que Luis escriba algo.   
    
    Ana escribe "Hola". El hilo de Ana sale de readLine() con linea = "Hola"
    broadcast recorre la lista de clientes conectados y llama a enviar("Ana: Hola") en cada uno:
        En el ClientHandler de Ana → escribe "Ana: Hola" por el writer de Ana.
        En el ClientHandler de Luis → escribe "Ana: Hola" por el writer de Luis.
    El el hilo de Ana está ejecutando el método enviar() que está dentro del objeto ClientHandler de Luis. 
    El hilo de Luis no se movió de su readLine(); fue el hilo de Ana el que entró en enviar() de Luis para mandarle el mensaje.

    Si Luis escribe que tal ahora sale del bloqueo del reader.readLine()
    y se ejecuta su propio enviar como son dos clientes de CLientHandler comparten writer para que no se sobrescriban y haya problemas se utiliza:
    synchronized (writer) {
        writer.println(mensaje);
    }
    Synchronized se encarga de ir turnando el trabajo
    */
}
