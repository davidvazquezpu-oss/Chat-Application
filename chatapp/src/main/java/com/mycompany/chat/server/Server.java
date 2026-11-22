package com.mycompany.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    /* ¿Que es un socket?
    Es el "enchufe" de comunicacion virtual (formado por IP + Puerto)
    que permite que dos programas se envíen y reciban datos a través de la red.    */

    /*Preparamos la base del servidor de nuestro chat
    PORT :Es una constante ,El cliente tendrá que usar este mismo número para conectarse. FASE1 */
    private static final int PORT = 5000;

    /*Una lista nueva de clientes utilizando la clase ClientHandler de la FASE 2
    final": la variable siempre apunta a la misma lista; lo que cambia es su contenido.
    -El tipo de la variable es List (la interfaz) y el objeto es un CopyOnWriteArrayList:
        AL declarar a variable como List<ClientHandler>, puedes usar todos los métodos estándar de List (como .add(), .remove(), .size(), .get(), o recorrerla con un for-each),
        y por debajo se ejecutan con la lógica de CopyOnWriteArrayList.
    Como funciona un CopyOnWriteArrayList:

    Característica	                    ArrayList	                                       
    Seguridad entre hilos	            No es seguro por sí solo; si un hilo recorre y otro modifica, puede fallar (ConcurrentModificationException) o dar datos inconsistentes.	Seguro para múltiples hilos al recorrer y modificar concurrentemente.
    Al modificar (.add / .remove)	    Modifica directamente sobre su array interno.	Copia todo el array en cada modificación (más costoso si modificas muchísimo).
    Al recorrer / leer	                Muy rápido, pero sensible a cambios simultáneos.	Muy rápido y seguro: recorre su copia/array estable sin chocar con escrituras.
    Cuándo conviene	                    Un solo hilo, o colecciones gestionadas de forma segura sin concurrencia directa.	Muchas lecturas/recorridos y pocas modificaciones (ideal para listas de clientes conectados en un chat).
   
                                        CopyOnWriteArrayList
    Seguridad entre hilos	            Seguro para múltiples hilos al recorrer y modificar concurrentemente.
    Al modificar (.add / .remove)	    Copia todo el array en cada modificación (más costoso si modificas muchísimo).
    Al recorrer / leer	                Muy rápido y seguro: recorre su copia/array estable sin chocar con escrituras.
    Cuándo conviene
    
    Lista con clientes es mutable*/
    private final List<ClientHandler> clientes = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        new Server().iniciar();
    }
    public void iniciar() {
        /*creamos un socket de servidor y lo vinculamos al puerto especificado (5000) 
        y el s.o. empezará escuchar peticiones de conexión entrantes.
         
        La variable serverSocket es la encargada de aceptar nuevas conexiones de clientes en el servidor 
        (aunque luego los mensajes viajen por el Socket individual de cada cliente), 
        y la dirección completa para encontrar ese servidor se forma uniendo la IP del equipo más el puerto 5000."
        */
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor escuhando puerto " + PORT);
             
            /* DIFERENCIA CLAVE CON LA FASE 1: ahora hay un bucle infinito para encargarse de cada cliente  */
            while (true) {
 
                /* accept() BLOQUEA el hilo main hasta que alguien se conecta. Y devuelve el Socket de ESE cliente. */
                Socket socket = serverSocket.accept();
                /*Una vez el  usuario se conecto introduciendo el PORT 
                socket guarda sus metadatos :

                IP del cliente: 	Dirección desde la que conectó	127.0.0.1 (Terminal B, tu PC osea la misma que Terminal A)
                Puerto del cliente:	Puerto efímero asignado por el SO	54321 (cambia cada conexión)
                IP del servidor	:   Dirección local del servidor	127.0.0.1
                Puerto del servidor	Puerto donde escucha	5000
                InputStream:    	Canal para leer datos del cliente	socket.getInputStream()
                OutputStream:	    Canal para escribir datos al cliente	socket.getOutputStream()*/
                
                /* try/catch INTERIOR: 
                    el constructor de ClientHandler puede lanzar IOException (al abrir los flujos). 
                    Si falla, solo se descarta ese cliente y el servidor sigue vivo para el siguiente. Si no lo
                   capturaras aqui, saltaria al catch de fuera y TODO el servidor dejaría de funcionar*/
                try {
                    ClientHandler handler = new ClientHandler(socket, this); //this signfica el server  en el que nos encotramos trabajando
 
                    /* Se anade a la lista ANTES de start(). Para evitar conflictos entre los hilos del main y del hilo del cliente*/
                    clientes.add(handler);
 
                    /* Crea un hilo nuevo y lo arranca para que atienda a ese cliente en paralelo mientras el servidor sigue escuchando a más gente
                       new Thread(handler) (el "cocinero" y la "receta")
                       handler (ClientHandler) implementa Runnable (porque tiene el método run()). Esa es la tarea/receta que hay que ejecutar para atender a ese cliente.
                       new Thread(...) crea un hilo nuevo (un "trabajador" o "cocinero" independiente en Java) y le entrega handler para que sepa qué trabajo tiene que hacer. */
                    new Thread(handler).start();//start arranca el hilo de handler

                    //si arrancamos el hilo a trabajar antes de agregarlo a lista de clientes podria darse el caso de que se borrase y luego se añadiera danddo fallo
 
                    System.out.println("Cliente conectado: " + socket.getInetAddress()
                            + " | Total conectados: " + clientes.size());
 
                } catch (IOException e) {
                    System.err.println("Error al preparar al cliente: " + e.getMessage());
                    cerrarSocketIndividual(socket);
                }
            }
 
        }catch (Exception e) {
            // Puede fallar, por ejemplo, si el puerto ya está en uso.
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
    /*  Envia el mensaje a todos los clientes conectados
    No necesita synchronized propio: la lista ya es segura para recorrerla y
    enviar() protege la escritura de cada cliente
    */
    void broadcast(String mensaje) {
        for (ClientHandler handler : clientes) {
            handler.enviar(mensaje);
        }
    }
 
    /* Lo llama el finally de ClientHandler cuando un cliente se va.
       remove() devuelve true solo si realmente estaba en la lista. */
    void remover(ClientHandler handler) {
        if (clientes.remove(handler)) {
            System.out.println("Cliente desconectado. Quedan " + clientes.size());
        }
    }
 
    //Ese método sirve para intentar cerrar el socket de un cliente que ha fallado al prepararse, sin que un error al cerrarlo tumbe todo el servidor.
    private static void cerrarSocketIndividual(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // Ignorado a proposito.
        }
    }    
}
   

