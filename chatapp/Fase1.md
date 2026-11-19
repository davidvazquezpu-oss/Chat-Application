# 📘 Fase 1 — El "Teléfono" Básico (Conexión 1-a-1 en Consola)

> **Objetivo:** Demostrar que dos programas Java independientes pueden intercambiar un mensaje de texto por red TCP y ambos lo ven en su consola.

---

## 📑 Índice

1. [Escenario de la Fase 1](#-escenario-de-la-fase-1)
2. [Secuencia completa de la conexión (8 pasos)](#-secuencia-completa-de-la-conexión-paso-a-paso)
3. [Creación de `Server.java`](#-creación-de-serverjava)
4. [**Creación de `NetworkClient.java`**](#-creación-de-networkclientjava) ⬅️ NUEVO
5. [Prueba de fuego](#-prueba-de-fuego-de-la-fase-1)
6. [Conceptos clave](#-conceptos-clave-de-la-fase-1)
7. [Errores típicos](#-errores-típicos-y-soluciones)
8. [Checklist de salida](#-checklist-de-salida-de-la-fase-1)

---

## 🖥️ Escenario de la Fase 1

```
┌─────────────────────────────────────────────────────┐
│  TU PC (un solo ordenador, un solo usuario)         │
│  IP: 127.0.0.1 (localhost)                          │
│                                                     │
│  ┌──────────────────┐      ┌──────────────────┐     │
│  │  TERMINAL A      │      │  TERMINAL B      │     │
│  │                  │      │                  │     │
│  │  Server.java     │◄════►│  NetworkClient   │     │
│  │  (servidor)      │      │  (cliente)       │     │
│  │                  │      │                  │     │
│  │  :5000           │      │  puerto efímero  │     │
│  └──────────────────┘      └──────────────────┘     │
│                                                     │
│  Mismo usuario, mismo PC, misma IP                  │
└─────────────────────────────────────────────────────┘
```

### 📌 Qué representa este diagrama

| Elemento | Qué es |
|----------|--------|
| **TU PC** | Un solo ordenador, un solo usuario (tú) |
| **127.0.0.1** | IP de loopback = "yo mismo". El tráfico no sale a la red |
| **TERMINAL A** | Ventana donde corre el **servidor** (`Server.java`) |
| **TERMINAL B** | Ventana donde corre el **único cliente** (`NetworkClient.java`) |
| **`:5000`** | Puerto donde el servidor escucha (fijo) |
| **Puerto efímero** | Puerto aleatorio que el SO asigna al cliente (ej: `54321`) |
| **`◄════►`** | Canal TCP bidireccional entre ambos procesos |

### 🧠 Aclaraciones clave

- **La terminal NO es el servidor**: la terminal es la ventana; el servidor es el proceso Java que corre dentro.
- **En Fase 1 hay 1 servidor + 1 cliente**, no "cliente 1 y cliente 2". Los múltiples clientes son **Fase 2**.
- **Ambos procesos están en el mismo PC**, pero son **procesos independientes** que se comunican por red (loopback).
- **El `ServerSocket` (portero) sigue vivo** aunque `accept()` haya devuelto un `Socket`. En Fase 1 no lo aprovechamos, pero en Fase 2 sí.

---

## 🎬 Secuencia completa de la conexión (paso a paso)

### 1️⃣ Terminal A: `serverSocket.accept()` ⏸️

```java
Socket socket = serverSocket.accept();  // ⏸️ SE BLOQUEA AQUÍ
```

**Qué pasa:**
- El `ServerSocket` está abierto en el puerto `5000`.
- `accept()` **bloquea el hilo `main`** del servidor.
- La terminal A sigue viva (acepta `Ctrl+C`), el puerto sigue escuchando.
- **Solo el hilo está esperando**, no el proceso entero.

**Salida en Terminal A:**
```
🟢 Servidor esperando cliente en el puerto 5000...
```

---

### 2️⃣ Terminal B: `new Socket("127.0.0.1", 5000)`

```java
Socket socket = new Socket("127.0.0.1", 5000);
```

**Qué pasa:**
- El cliente toma la **iniciativa**: pide al SO conectar al servidor.
- El SO del cliente hace el **handshake TCP**:
  ```
  Cliente ──── SYN ────► Servidor
  Cliente ◄─── SYN-ACK ── Servidor
  Cliente ──── ACK ────► Servidor
  ```
- El SO del cliente asigna un **puerto efímero** (ej: `54321`).
- En milisegundos, la conexión está establecida.

---

### 3️⃣ Terminal A: `accept()` se desbloquea ⏩

```java
Socket socket = serverSocket.accept();  // ⏩ DEVUELVE UN Socket
```

**Qué pasa:**
- `accept()` **devuelve un `Socket` NUEVO** (no es el `ServerSocket`).
- Ese `Socket` representa **la conexión con ESE cliente concreto**.

**Qué contiene el `Socket` devuelto:**

```
Socket
├── IP cliente: 127.0.0.1          ← getInetAddress()
├── Puerto cliente: 54321          ← getPort()  (efímero, cambia cada vez)
├── IP servidor: 127.0.0.1         ← getLocalAddress()
├── Puerto servidor: 5000          ← getLocalPort()
├── InputStream (para leer)        ← getInputStream()
└── OutputStream (para escribir)   ← getOutputStream()
```

> ⚠️ **El `ServerSocket` (portero) sigue escuchando** en el puerto 5000. El `Socket` devuelto es el "teléfono descolgado" para hablar con ese cliente.

---

### 4️⃣ Terminal A: `println("Cliente conectado: " + socket.getInetAddress())`

```java
System.out.println("Cliente conectado: " + socket.getInetAddress());
```

**Qué pasa:**
- `getInetAddress()` devuelve un objeto `InetAddress` con la IP del **cliente**.
- En Fase 1, como el cliente está en el **mismo PC**, la IP es `127.0.0.1`.
- `InetAddress.toString()` añade un `/` delante por convención.

**Salida en Terminal A:**
```
✅ Cliente conectado: /127.0.0.1
```

> 💡 Si quieres la IP limpia (sin `/`), usa `.getHostAddress()`:
> ```java
> System.out.println("Cliente conectado: " + socket.getInetAddress().getHostAddress());
> // → Cliente conectado: 127.0.0.1
> ```

---

### 5️⃣ Terminal A: `atenderCliente(socket)`

```java
private static void atenderCliente(Socket socket) {
    try (socket;
         BufferedReader reader = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
         PrintWriter writer = new PrintWriter(
                 socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

        String linea = reader.readLine();  // ⏸️ ESPERA
        ...
    }
}
```

**Qué pasa:**
- Se abren los **canales de datos** sobre el `Socket`:
  - `BufferedReader` sobre `getInputStream()` → para **leer**.
  - `PrintWriter` sobre `getOutputStream()` → para **escribir**.
- `reader.readLine()` **se bloquea** esperando el mensaje del cliente.
- `try-with-resources` garantiza que al salir se cierren `writer`, `reader` y `socket` (en ese orden).

> 🔑 **`try-with-resources`**: los recursos van en los `()` del `try` y se cierran **automáticamente** al salir del bloque, incluso si hay excepción.

---

### 6️⃣ Terminal B: escribe "Hola" y pulsa Enter

```java
String mensaje = teclado.nextLine();  // "Hola"
writer.println(mensaje);
```

**Qué pasa:**
- El cliente lee del teclado con `Scanner`.
- `writer.println("Hola")` envía el texto **con un `\n` al final**.
- Con `autoFlush = true`, se envía **al instante** por el socket.

---

### 7️⃣ Terminal A: `readLine()` devuelve "Hola"

```java
String linea = reader.readLine();  // ⏩ DEVUELVE "Hola"
System.out.println("Recibido: " + linea);
writer.println("Mensaje recibido");
```

**Qué pasa:**
- `reader.readLine()` **se desbloquea** al recibir la línea completa.
- Devuelve `"Hola"` **sin** el `\n` final.
- El servidor imprime el mensaje recibido.
- El servidor responde con `writer.println("Mensaje recibido")`.

**Salida en Terminal A:**
```
📩 Mensaje recibido del cliente: Hola
```

> ⚠️ **`readLine()` puede devolver `null`**: significa que el cliente cerró la conexión sin enviar nada. Hay que comprobarlo antes de usar la variable.

---

### 8️⃣ Terminal B: `readLine()` devuelve "Mensaje recibido"

```java
String respuesta = reader.readLine();  // ⏩ DEVUELVE "Mensaje recibido"
System.out.println("Servidor responde: " + respuesta);
```

**Qué pasa:**
- El cliente estaba bloqueado en `reader.readLine()` esperando la respuesta.
- Al llegar, la imprime.
- El servidor cierra la conexión (sale del `try-with-resources`).

**Salida en Terminal B:**
```
📨 Servidor responde: Mensaje recibido
```

**Salida final en Terminal A:**
```
🟢 Servidor esperando cliente en el puerto 5000...
✅ Cliente conectado: /127.0.0.1
📩 Mensaje recibido del cliente: Hola
📤 Respuesta enviada. Cerrando conexión.
```

---

## 📊 Resumen visual de los 8 pasos

```
TIEMPO   TERMINAL A (servidor)              TERMINAL B (cliente)
──────   ─────────────────────              ────────────────────

1        accept() ⏸️
         (bloqueado en :5000)

2                                            new Socket("127.0.0.1", 5000)
                                             └── handshake TCP

3        accept() ⏩
         └── devuelve Socket

4        println("Cliente conectado: /127.0.0.1")

5        atenderCliente(socket)
         ├── abre streams
         └── readLine() ⏸️

6                                            escribe "Hola"
                                             └── writer.println("Hola")

7        readLine() ⏩ "Hola"
         ├── println("Recibido: Hola")
         └── writer.println("Mensaje recibido")

8                                            readLine() ⏩ "Mensaje recibido"
                                             └── println("Servidor responde: ...")
```

---

## 🖥️ Creación de `Server.java`

**Ruta:** `src/main/java/com/mycompany/chat/server/Server.java`

### 🧠 Puntos clave del servidor

| Elemento | Función |
|----------|---------|
| `ServerSocket(5000)` | Abre el puerto 5000 y escucha |
| `accept()` | Bloquea el hilo hasta que llega un cliente |
| `atenderCliente(socket)` | Gestiona la conversación con **ese** cliente |
| `try-with-resources` | Cierra `socket`, `reader`, `writer` automáticamente |
| `linea == null` | Detecta desconexión sin mensaje |
| `catch (IOException)` | No rompe el servidor si hay error |

> 💡 En Fase 2, `atenderCliente()` se moverá casi tal cual al `run()` de `ClientHandler`, y el `main` pasará a tener un `while(true)` con `new Thread(...).start()`.

---

## 📞 Creación de `NetworkClient.java`

**Ruta:** `src/main/java/com/mycompany/chat/client/NetworkClient.java`

> ⚠️ **`NetworkClient` es obligatorio en Fase 1.** Sin cliente, el servidor se queda bloqueado en `accept()` para siempre.

### ¿Por qué es necesario?

```
Sin cliente:                          Con cliente:
┌──────────────┐                      ┌──────────────┐      ┌──────────────┐
│  Terminal A  │                      │  Terminal A  │      │  Terminal B  │
│  Server      │                      │  Server      │◄════►│  NetworkClient│
│  accept() ⏸️ │                      │  accept() ⏩ │      │  new Socket() │
│  (para siempre)│                    │  lee/responde│      │  envía/lee    │
└──────────────┘                      └──────────────┘      └──────────────┘
```

👉 **El servidor es pasivo: espera. El cliente es activo: conecta.** Sin el segundo, el primero no hace nada.

### Código completo

```java
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
        String ipServidor = "127.0.0.1";
        int puertoServidor = 5000;

        try (Socket socket = new Socket(ipServidor, puertoServidor);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                     socket.getOutputStream(), true, StandardCharsets.UTF_8);
             Scanner teclado = new Scanner(System.in)) {

            System.out.println("✅ Conectado al servidor " + ipServidor + ":" + puertoServidor);
            System.out.print("👉 Escribe tu mensaje: ");

            // 1. Leer del teclado
            String mensaje = teclado.nextLine();

            // 2. Enviar al servidor
            writer.println(mensaje);
            System.out.println("📤 Enviado: " + mensaje);

            // 3. Leer la respuesta del servidor (bloquea hasta que llegue)
            String respuesta = reader.readLine();
            System.out.println("📨 Servidor responde: " + respuesta);

        } catch (IOException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            System.err.println("   ¿Está el servidor arrancado en " + ipServidor + ":" + puertoServidor + "?");
        }
    }
}
```

### 🧠 Explicación de cada parte del cliente

#### 🔹 1. `new Socket(ipServidor, puertoServidor)`

```java
Socket socket = new Socket("127.0.0.1", 5000);
```

- **Toma la iniciativa**: pide al SO conectar al servidor.
- El SO hace el **handshake TCP** (SYN → SYN-ACK → ACK).
- **Desbloquea el `accept()`** del servidor.
- Si el servidor no está escuchando → `ConnectException` **en milisegundos**.

#### 🔹 2. Los canales de datos

```java
BufferedReader reader = new BufferedReader(
        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
PrintWriter writer = new PrintWriter(
        socket.getOutputStream(), true, StandardCharsets.UTF_8);
```

| Canal | Método | Para qué |
|-------|--------|----------|
| **Entrada** | `getInputStream()` | Leer la respuesta del servidor |
| **Salida** | `getOutputStream()` | Enviar el mensaje al servidor |

📌 **`true` en el `PrintWriter`** = `autoFlush`. Sin él, el mensaje se queda en el búfer y **no se envía** hasta que se llene o se cierre.

#### 🔹 3. `Scanner` para leer del teclado

```java
Scanner teclado = new Scanner(System.in);
String mensaje = teclado.nextLine();
```

- `System.in` = la entrada estándar (tu teclado).
- `nextLine()` **bloquea** hasta que pulsas Enter.
- Devuelve la línea **sin** el `\n`.

#### 🔹 4. Enviar y recibir

```java
writer.println(mensaje);                // enviar
String respuesta = reader.readLine();   // recibir (bloquea)
```

- `println()` añade `\n` → el `readLine()` del servidor sabrá que la línea terminó.
- `reader.readLine()` **se bloquea** hasta que el servidor responda.

#### 🔹 5. `try-with-resources` cierra todo

```java
try (Socket socket = ...;
     BufferedReader reader = ...;
     PrintWriter writer = ...;
     Scanner teclado = ...) { ... }
```

Al salir (normal o por excepción):
1. Se cierra `teclado` (Scanner).
2. Se cierra `writer` (PrintWriter).
3. Se cierra `reader` (BufferedReader).
4. Se cierra `socket` (Socket).

📌 **El orden es inverso** al de declaración: no puedes cerrar el socket antes que los streams que lo usan.

### ⚠️ Lo que **NO** debe hacer `NetworkClient` en Fase 1

| ❌ No hacer | ¿Por qué? | ¿Cuándo? |
|-------------|-----------|----------|
| Hilo de escucha (`new Thread(...)`) | Eso es Fase 2 | Fase 2 |
| Bucle `while(true)` para enviar varios mensajes | El servidor solo atiende 1 | Fase 2 |
| `Consumer<String>` | Desacoplar el cliente | Fase 3 |
| JavaFX | Ventana gráfica | Fase 4–5 |
| `main` "temporal" con `Scanner` | ✅ **Correcto en Fase 1** | Fase 3 se quita |

👉 **En Fase 1, `NetworkClient` es un `main` temporal**: se usa solo para probar que el servidor funciona. En Fase 3 se refactoriza para quitar el `main` y desacoplarlo con `Consumer<String>`.

### 🧪 Experimentos con `NetworkClient`

#### Experimento 1 — Cliente sin servidor
1. **No arranques** el servidor.
2. Arranca el cliente.
3. ✅ Debe mostrar:
   ```
   ❌ Error de conexión: Connection refused: connect
      ¿Está el servidor arrancado en 127.0.0.1:5000?
   ```

👉 **Demuestra que el cliente necesita al servidor.**

#### Experimento 2 — Cliente que no envía nada
1. Arranca el servidor.
2. Arranca el cliente y pulsa Enter **sin escribir nada**.
3. El servidor recibe una línea vacía (`""`).
4. El servidor responde igualmente `"Mensaje recibido"`.

👉 **Demuestra que `readLine()` devuelve `""` (no `null`) si se envía una línea vacía.**

#### Experimento 3 — Ver los puertos
Añade en el cliente, justo después de `new Socket(...)`:
```java
System.out.println("Mi puerto local: " + socket.getLocalPort());
System.out.println("Puerto del servidor: " + socket.getPort());
```
Verás algo como:
```
Mi puerto local: 54321
Puerto del servidor: 5000
```

👉 **Demuestra que el cliente usa un puerto efímero y el servidor el 5000.**

#### Experimento 4 — Cerrar el cliente abruptamente
1. Arranca servidor y cliente.
2. En el cliente, pulsa `Ctrl+C` **antes** de enviar nada.
3. En el servidor, `readLine()` devuelve `null`.
4. El servidor imprime: `"El cliente se desconectó sin enviar nada."`

👉 **Demuestra el caso `null` que ya tienes manejado en `atenderCliente()`.**

### 📊 Comparación: `Server.java` vs `NetworkClient.java` (Fase 1)

| Aspecto | `Server.java` | `NetworkClient.java` |
|---------|---------------|----------------------|
| **Clase que usa** | `ServerSocket` + `Socket` | `Socket` (solo) |
| **¿Quién inicia?** | ❌ Espera (`accept()`) | ✅ **Inicia** (`new Socket()`) |
| **¿Se bloquea?** | ✅ En `accept()` | ⚠️ Solo en `new Socket()` (milisegundos) |
| **¿Cuántos hay?** | 1 | 1 (en Fase 1) |
| **¿Lee del teclado?** | ❌ No | ✅ Sí (`Scanner`) |
| **¿Cuándo termina?** | Tras atender 1 cliente | Tras enviar y recibir 1 mensaje |
| **Rol** | Pasivo (espera) | **Activo (conecta)** |

### ✅ Checklist específico de `NetworkClient`

- [ ] Archivo creado en `com.mycompany.chat.client`.
- [ ] Compila con `mvn clean compile`.
- [ ] Conecta al servidor y muestra "Conectado al servidor".
- [ ] Lees del teclado con `Scanner`.
- [ ] Envías el mensaje con `writer.println()`.
- [ ] Recibes la respuesta con `reader.readLine()`.
- [ ] Provocas `ConnectException` sin servidor.
- [ ] Provocas `null` cerrando el cliente sin enviar.
- [ ] Entiendes **qué hace cada línea** del cliente.

---

## 🔥 Prueba de fuego de la Fase 1

### Terminal A — Servidor
```bash
mvn exec:java -Dexec.mainClass="com.mycompany.chat.server.Server"
```
Salida:
```
🟢 Servidor esperando cliente en el puerto 5000...
```
🛑 **No lo cierres.** Se queda colgado en `accept()`.

### Terminal B — Cliente
```bash
mvn exec:java -Dexec.mainClass="com.mycompany.chat.client.NetworkClient"
```
Salida:
```
✅ Conectado al servidor 127.0.0.1:5000
👉 Escribe tu mensaje: 
```
Escribe `Hola` y pulsa Enter.

### Resultado esperado

**Terminal A (servidor):**
```
🟢 Servidor esperando cliente en el puerto 5000...
✅ Cliente conectado: /127.0.0.1
📩 Mensaje recibido del cliente: Hola
📤 Respuesta enviada. Cerrando conexión.
```

**Terminal B (cliente):**
```
📤 Enviado: Hola
📨 Servidor responde: Mensaje recibido
```

🎉 **Si ves esto, Fase 1 superada.**

---

## 🧠 Conceptos clave de la Fase 1

### 1. `ServerSocket` vs `Socket`

| | `ServerSocket` | `Socket` |
|--|----------------|----------|
| **Qué representa** | El "portero" que escucha | El canal con UN cliente |
| **Quién lo usa** | Solo el servidor | Servidor **y** cliente |
| **Método clave** | `accept()` | `getInputStream()` / `getOutputStream()` |
| **¿Sirve para leer/escribir?** | ❌ No | ✅ Sí |
| **¿Cuántos hay?** | 1 por servidor | 1 por conexión |

### 2. `accept()` **bloquea el hilo**, no la terminal

- **La terminal sigue viva** (acepta `Ctrl+C`).
- **El puerto sigue escuchando** (verificable con `netstat`).
- **Solo el hilo `main`** está bloqueado esperando.

### 3. El cliente **toma la iniciativa**

- El servidor **espera pasivamente** en `accept()`.
- El cliente **actúa activamente** con `new Socket(...)`.
- Si el cliente arranca antes que el servidor → `ConnectException: Connection refused`.

### 4. `try-with-resources`

- Los recursos van en los `()` del `try`.
- Se cierran **automáticamente** al salir del bloque.
- Se cierran en **orden inverso** al de declaración.
- Cierran **incluso si hay excepción**.

### 5. Puertos

| Puerto | Qué es | Ejemplo |
|--------|--------|---------|
| **Puerto del servidor** | Fijo, elegido por ti | `5000` |
| **Puerto del cliente** | Efímero, asignado por el SO | `54321` (cambia cada conexión) |

---

## ⚠️ Errores típicos y soluciones

| Error | Causa | Solución |
|-------|-------|----------|
| `ConnectException: Connection refused` | Servidor no está corriendo | Arranca `Server` primero |
| `BindException: Address already in use` | Otro proceso usa el puerto 5000 | Cambia de puerto o mata el proceso anterior |
| Se queda colgado en `readLine()` | El otro extremo no envía `\n` | Usa `println()` (con `\n`), no `print()` |
| Caracteres raros (ñ, tildes) | Charset por defecto del sistema | Usa `StandardCharsets.UTF_8` en ambos lados |
| El servidor termina tras 1 mensaje | **Comportamiento esperado en Fase 1** | Se arregla en Fase 2 con `while(true)` |

---

## ✅ Checklist de salida de la Fase 1

- [ ] El proyecto compila con `mvn clean compile` sin errores.
- [ ] `Server.java` creado en `com.mycompany.chat.server`.
- [ ] **`NetworkClient.java` creado en `com.mycompany.chat.client`.**
- [ ] `Server` se queda esperando en `accept()` al arrancar.
- [ ] `NetworkClient` se conecta y envía un mensaje.
- [ ] El servidor imprime el mensaje recibido.
- [ ] El cliente recibe y muestra `"Mensaje recibido"`.
- [ ] Provocas `ConnectException` al arrancar el cliente sin servidor.
- [ ] Provocas `BindException` al arrancar dos servidores.
- [ ] Entiendes **qué hace cada línea** de `Server.java` y `NetworkClient.java`.
- [ ] Guardaste el commit `fase-1-completada`.

---

## 🎓 En una frase

> **Fase 1 = 1 PC, 2 terminales (A = servidor, B = 1 cliente), 1 conexión TCP por `127.0.0.1:5000`, 1 mensaje de ida y vuelta, y el servidor muere. Es la base absoluta de toda comunicación en red.**

---

## 🚀 Siguiente paso

Cuando termines la Fase 1, el siguiente paso es la **Fase 2 (multiusuario con hilos)**, donde:

- `accept()` se mete en un `while(true)`.
- Cada cliente tendrá su propio `ClientHandler` (un hilo).
- Los mensajes se reenvían a todos con `broadcast()`.
- El servidor **ya no muere** tras atender a un cliente.
- **`NetworkClient` cambiará**: tendrá un hilo de escucha + un hilo de envío.

Pero eso ya es otra fase. **Primero, celebra que tu "teléfono" funciona.** 📞