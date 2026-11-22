package com.mycompany.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class NetworkClient {

    public static void main(String[] args) {
        String ipServidor = "127.0.0.1"; //Es invent
        int puertoServidor = 5000;

        try (Socket socket = new Socket(ipServidor, puertoServidor);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                     socket.getOutputStream(), true, StandardCharsets.UTF_8);
            Scanner teclado = new Scanner(System.in)) {

            System.out.println("Conectado al servidor con ip: " + ipServidor + " con puerto :" + puertoServidor);
            //FASE 2:                        
            System.out.print(" Escribe tu username: ");
            String nombre= teclado.nextLine();    
            writer.println(nombre); // Enviar al servidor
            
            //hilo del clientHandler
            Thread hiloEscucha = new Thread(new Escuchador(reader, socket));
            hiloEscucha.setDaemon(true);
            hiloEscucha.start();
            
            //hilo del main
            while (teclado.hasNextLine()) {
                String mensaje = teclado.nextLine();
                if ("/salir".equalsIgnoreCase(mensaje)) {
                    break;
                }
                writer.println(mensaje);
            }

        } catch (IOException e) {
            System.err.println(" Error de conexión: " + e.getMessage());
            System.err.println("   ¿Está el servidor arrancado en " + ipServidor + ":" + puertoServidor + "?");
        }
    }
    static class Escuchador implements Runnable {

        private final BufferedReader reader;
        private final Socket socket;

        Escuchador(BufferedReader reader, Socket socket) {
            this.reader = reader;
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                //bucle de lectura de mensajes del servidor
                String linea;
                while ((linea = reader.readLine()) != null) {
                    System.out.println(linea);
                }

                //El servidor cerro la conexion (readLine devolvio null)
                System.out.println("El servidor cerro la conexion.");

            } catch (IOException e) {
                // Distinguir salida voluntaria (/salir) de caida real
                if (!socket.isClosed()) {
                    System.err.println("Se perdio la conexion: " + e.getMessage());
                }
            }
        }
    }
}