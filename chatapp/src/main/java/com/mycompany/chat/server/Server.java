package com.mycompany.chat.server;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    /* ¿Que es un socket?
    Es el "enchufe" de comunicacion virtual (formado por IP+ Puerto)
    que permite que dos programas se envíen y reciban datos a través de la red.
    
    En la Fase 1 no trabajaremos con IP todavía 
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
            
            //devuelve conexion IP del cliente este caso será la misma porque en la fase 1 es mismo PC
            System.out.println("Cliente conectado: " + socket.getInetAddress());

            atenderCliente(socket);
        }catch (Exception e) {
            // Puede fallar, por ejemplo, si el puerto ya está en uso.
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    private static void atenderCliente(Socket socket){

    }
}
   

