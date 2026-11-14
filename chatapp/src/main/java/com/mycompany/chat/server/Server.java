package com.mycompany.chat.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    /*Establece una constante con el número de puerto del ordenador donde el servidor escuchará las conexiones. */
    private static final int PORT = 5000;
    //List<ClientHandler> (por ejemplo, un ArrayList) causaría graves problemas de concurrencia y errores en memoria en una aplicación multi-hilo como este servidor.
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    /*En tu servidor, cada cliente corre en su propio hilo independiente (new Thread(handler).start()).
    Esto significa que varias cosas pueden ocurrir exactamente al mismo milisegundo:
    Un cliente nuevo se conecta y se añade a la lista.
    Otro cliente se desconecta y se elimina de la lista.
    El servidor está recorriendo la lista para enviar un mensaje (broadcast) a todos. */
}
   

