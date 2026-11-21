package com.mycompany.chat.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Server {
    /* ¿Que es un socket?
    Es el "enchufe" de comunicacion virtual (formado por IP + Puerto)
    que permite que dos programas se envíen y reciban datos a través de la red.
    
    En la Fase 1 no trabajaremos con IP real todavía 
    */

    /*Preparamos la base del servidor de nuestro chat
    PORT :Es una constante ,El cliente tendrá que usar este mismo número para conectarse.*/
    private static final int PORT = 5000;

    public static void main(String[] args) {
        /*creamos un socket de servidor y lo vinculamos al puerto especificado (5000) 
        y el s.o. empezará escuchar peticiones de conexión entrantes.
         
        La variable serverSocket es la encargada de aceptar nuevas conexiones de clientes en el servidor 
        (aunque luego los mensajes viajen por el Socket individual de cada cliente), 
        y la dirección completa para encontrar ese servidor se forma uniendo la IP del equipo más el puerto 5000."
        */
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor esperando cliente en el puerto " + PORT);
            System.out.println("Esperando ...");

            /*"El .accept() es el encargado de bloquear el hilo principal del servidor (que corre en la terminal A osea el proceso de la clase Server) 
            hasta que un cliente se conecte desde otro proceso ( terminal B). 
            Mientras tanto, la terminal A sigue viva y el puerto 5000 sigue escuchando, pero el hilo main no avanza." */
            Socket socket = serverSocket.accept();

            /*Una vez el segudo usuario se conecto introduciendo el PORT 
            socket guarda sus metadatos :

            IP del cliente: 	Dirección desde la que conectó	127.0.0.1 (Terminal B, tu PC osea la misma que Terminal A)
            Puerto del cliente:	Puerto efímero asignado por el SO	54321 (cambia cada conexión)
            IP del servidor	:   Dirección local del servidor	127.0.0.1
            Puerto del servidor	Puerto donde escucha	5000
            InputStream:    	Canal para leer datos del cliente	socket.getInputStream()
            OutputStream:	    Canal para escribir datos al cliente	socket.getOutputStream()
            
            
            */

            //devuelve conexion IP del cliente este caso será la misma porque en la fase 1 es mismo PC
            System.out.println("Cliente conectado: " + socket.getInetAddress());

            atenderCliente(socket);
        }catch (Exception e) {
            // Puede fallar, por ejemplo, si el puerto ya está en uso.
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
    /*El método que se encarga de "la conversación" con un cliente concreto: abrir los canales de datos, leer lo que envía, responderle y cerrar limpiamente */
    private static void atenderCliente(Socket socket){
        //Utizamos un try-with-resource
        try (socket;
        
        /*Sun buffers:
        que toma los bytes entrantes del cliente (socket.getInputStream()) y los convierte a caracteres usando codificación UFT_8*/
         BufferedReader reader = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
         PrintWriter writer = new PrintWriter(
                 socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
        //readLine() lee el buffer reader hasta en contrar \n y lo guarda en un string
        String linea = reader.readLine();
        if (linea == null) {
            System.out.println("El cliente se desconectó sin enviar nada.");
            return;
        }
        System.out.println("Mensaje recibido del cliente fue : \n" + linea);
        writer.println("Mensaje recibido");
            
        }catch (Exception e) {
            System.err.println("Error atendiendo al cliente: " + e.getMessage());
        }
    }
}
   

