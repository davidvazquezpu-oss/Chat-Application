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
            System.out.print(" Escribe tu mensaje: ");

            // 1. Leer del teclado
            String mensaje = teclado.nextLine();

            // 2. Enviar al servidor
            writer.println(mensaje);
            System.out.println("Enviado: " + mensaje);

            // 3. Leer la respuesta del servidor (bloquea hasta que llegue)
            String respuesta = reader.readLine();
            System.out.println(" Servidor responde: " + respuesta);

        } catch (IOException e) {
            System.err.println(" Error de conexión: " + e.getMessage());
            System.err.println("   ¿Está el servidor arrancado en " + ipServidor + ":" + puertoServidor + "?");
        }
    }
}