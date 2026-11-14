# 💬 Aplicación de Chat Cliente-Servidor (Java + Sockets + JavaFX)Rodrigo

Este proyecto está diseñado para construirse **paso a paso**. Primero crearemos una estructura minimalista dedicada únicamente al **motor del chat en consola** (Fases 1 a 3). Solo cuando ese motor funcione al 100%, ampliaremos las carpetas para añadir la **interfaz gráfica de JavaFX** (Fases 4 y 5).

---

## 📂 Etapa 1: Estructura Inicial (Solo el Motor en Consola)

Durante las **Fases 1, 2 y 3**, tu proyecto será muy limpio y sencillo. No verás ni una sola clase de ventanas o botones todavía:

```text
i-chat-app/
├── pom.xml                                  # Configuración básica de Maven
├── README.md                                # Esta guía paso a paso
└── src/
    └── main/
        └── java/
            └── com/
                └── miempresa/
                    └── chat/
                        ├── server/
                        │   ├── Server.java          # La "centralita" que recibe a todos
                        │   └── ClientHandler.java   # El "operador" que atiende a 1 cliente (Fase 2)
                        └── client/
                            └── NetworkClient.java   # El "teléfono" que envía y recibe datos en consola
```

---

## 🗺️ Hoja de Ruta Didáctica: Construyendo el Motor

### Fase 1: El "Teléfono" Básico (Conexión 1 a 1 en Consola)

#### 🎯 ¿Por qué hacemos esta fase?
Antes de crear un chat con muchas personas y ventanas bonitas, necesitas entender cómo dos programas distintos en Java pueden enviarse un simple texto entre sí. Si logras que un programa diga "Hola" y el otro responda "Recibido", ya tienes la base de internet.

#### 🛠️ Herramientas que vas a usar y cómo funcionan
* **`ServerSocket` (El timbre de la puerta):** Es una clase de Java que usa el **Servidor** para quedarse esperando en un puerto específico de tu ordenador (por ejemplo, el puerto `5000`). Cuando ejecuta `.accept()`, el programa se congela literalmente hasta que alguien "llama a la puerta".
* **`Socket` (El cable telefónico):** Es la conexión real entre el Cliente y el Servidor. Cuando el Cliente hace `new Socket("localhost", 5000)`, marca el número del servidor. Si el servidor acepta, ambos tienen un `Socket` conectado por donde viajarán los datos.
* **`PrintWriter` (El micrófono):** Es la herramienta para **enviar** texto por el cable (`Socket`). Funciona igual que `System.out.println()`, pero en lugar de escribir en tu pantalla, envía el texto por la red hacia el otro programa.
* **`BufferedReader` (El altavoz):** Es la herramienta para **escuchar** lo que llega por el cable (`Socket`). Cuando llamas a `.readLine()`, el programa espera hasta que el otro extremo envíe una línea de texto completa.

#### ✅ Tareas de la Fase 1
* [ ] **Crear el servidor mínimo en `Server.java`:**
  * [ ] Abrir un `ServerSocket(5000)`.
  * [ ] Esperar una conexión con `Socket cliente = serverSocket.accept()`.
  * [ ] Preparar el `BufferedReader` para leer lo que dice el cliente y el `PrintWriter` para responderle.
  * [ ] Leer una frase del cliente, mostrarla en consola y devolverle `"Mensaje recibido"`.
* [ ] **Crear el cliente mínimo en `NetworkClient.java` (con un `main` temporal):**
  * [ ] Conectarse con `new Socket("127.0.0.1", 5000)`.
  * [ ] Usar `Scanner` para leer lo que escribes en tu teclado.
  * [ ] Enviarlo con `PrintWriter` y leer la respuesta del servidor con `BufferedReader`.
* [ ] **Prueba de fuego:** Arranca `Server.java`, luego arranca `NetworkClient.java`, escribe algo y comprueba que se comunican.

---

### Fase 2: La "Centralita" Multiusuario (Concurrencia y Hilos)

#### 🎯 ¿Por qué hacemos esta fase?
En la Fase 1 notarás un problema grave: mientras el servidor habla con un cliente, ignora a todos los demás. Además, en tu cliente no puedes recibir mensajes mientras estás escribiendo. Para que varios usuarios hablen a la vez en tiempo real, necesitamos que Java haga varias cosas al mismo tiempo (**Multihilo**).

#### 🛠️ Herramientas que vas a usar y cómo funcionan
* **`Thread` y `Runnable` (Clonar trabajadores):** Normalmente, un programa en Java sigue una sola línea de instrucciones de arriba a abajo. Un `Thread` (hilo) es como contratar a un trabajador extra que ejecuta un bloque de código independiente en paralelo. `Runnable` es la "hoja de tareas" que le das a ese trabajador.
* **Bucle `while (true)` en el servidor:** El servidor principal ya no se detiene tras atender a un cliente; entra en un bucle infinito donde acepta a un cliente, le asigna un trabajador (`ClientHandler`) y vuelve inmediatamente a esperar al siguiente.
* **Listas sincronizadas (`CopyOnWriteArrayList` o bloques `synchronized`):** Como varios hilos van a leer y modificar la lista de clientes conectados exactamente al mismo milisegundo, necesitamos una lista "segura para hilos" para evitar que Java lance un error de choque de memoria.

#### ✅ Tareas de la Fase 2
* [ ] **Crear al trabajador `ClientHandler.java` (en `server`):**
  * [ ] Hacer que implemente `Runnable`.
  * [ ] Meter dentro de su método `run()` el bucle que escucha constantemente los mensajes de **un solo cliente**.
* [ ] **Convertir `Server.java` en un repartidor de mensajes:**
  * [ ] Poner `serverSocket.accept()` dentro de un `while (true)`.
  * [ ] Por cada cliente que entre, crear un `new ClientHandler(socket)` y arrancarlo en un hilo nuevo (`new Thread(handler).start()`).
  * [ ] Guardar todos los `ClientHandler` en una lista y crear un método `broadcast(String mensaje)` que recorra la lista y envíe el mensaje a todos.
* [ ] **Hacer que `NetworkClient.java` escuche y escriba a la vez:**
  * [ ] Crear un `Thread` secundario dentro del cliente que tenga un bucle `while` leyendo mensajes del servidor constantemente.
  * [ ] Dejar el hilo principal leyendo lo que tú escribes con `Scanner`.
* [ ] **Prueba de fuego:** Abre 1 servidor y 3 terminales de cliente en VS Code. Comprueba que lo que escribe uno lo leen los otros dos al instante.

---

### Fase 3: Desacoplar el Motor (Prepararlo para el futuro)

#### 🎯 ¿Por qué hacemos esta fase?
Ahora mismo tu `NetworkClient.java` usa `System.out.println()` para mostrar los mensajes que llegan. Pero cuando pasemos a la interfaz gráfica, la consola desaparecerá y los mensajes tendrán que ir a una ventana. En esta fase preparamos `NetworkClient` para que sea "ciego" a dónde van los mensajes: él solo avisa cuando llega algo.

#### 🛠️ Herramientas que vas a usar y cómo funcionan
* **`Consumer<String>` (Un buzón de aviso):** Es una interfaz sencilla de Java que representa una acción que acepta un `String` y no devuelve nada. En lugar de que `NetworkClient` imprima el texto por su cuenta, le pasamos un `Consumer<String>` (una función) para que él simplemente diga: *"Oye, ha llegado este texto, haz con él lo que quieras"*.

#### ✅ Tareas de la Fase 3
* [ ] **Limpiar `NetworkClient.java`:**
  * [ ] Quitar el `main` y los `Scanner` de esta clase.
  * [ ] Crear métodos claros: `conectar(String ip, int puerto, String usuario, Consumer<String> alRecibirMensaje)` y `enviarMensaje(String texto)`.
  * [ ] Cuando llegue una línea del servidor, llamar a `alRecibirMensaje.accept(linea)`.

---

## 📂 Etapa 2: Ampliación de Estructura (Añadiendo JavaFX)

**¡Solo cuando el motor de la Etapa 1 funcione perfectamente!** Ahora añadimos las dependencias de JavaFX al `pom.xml` y creamos las clases visuales dentro del paquete `client` (sin tocar nada de `server`):

```text
i-chat-app/
├── pom.xml                                  # Ahora añadimos las librerías de JavaFX
└── src/
    └── main/
        └── java/
            └── com/
                └── miempresa/
                    └── chat/
                        ├── server/
                        │   ├── Server.java          # (Intacto, ya funciona)
                        │   └── ClientHandler.java   # (Intacto, ya funciona)
                        └── client/
                            ├── NetworkClient.java   # (Intacto, ya funciona)
                            ├── ClientApp.java       # [NUEVO] La ventana principal de JavaFX
                            └── ChatController.java  # [NUEVO] El cerebro de los botones y pantallas
```

---

## 🗺️ Hoja de Ruta Didáctica: Agregando la Interfaz Gráfica

### Fase 4: Aprender JavaFX desde Cero (Solo Visual, sin Red)

#### 🎯 ¿Por qué hacemos esta fase?
Como no tienes experiencia con JavaFX, intentar conectarlo directamente al servidor sería muy confuso. Aquí vas a aprender a dibujar una ventana básica con botones y cajas de texto que funcionen de forma local, sin internet ni sockets.

#### 🛠️ Herramientas que vas a usar y cómo funcionan
* **`Application` y `Stage` (El escenario):** Toda app de JavaFX hereda de `Application`. El `Stage` es literalmente la ventana de Windows/Linux/Mac (con su barra de título y botones de cerrar/minimizar).
* **`Scene` y Contenedores (`BorderPane`, `HBox`):** La `Scene` es lo que va dentro de la ventana. Para ordenar los elementos visuales usamos contenedores:
  * `BorderPane`: Divide la pantalla en 5 zonas (Arriba, Abajo, Izquierda, Derecha y Centro). Ideal para poner el chat en el Centro y la barra de escribir Abajo.
  * `HBox` (Horizontal Box): Pone elementos uno al lado del otro en fila (ideal para poner la caja de texto a la izquierda y el botón "Enviar" a la derecha).
* **Controles (`TextArea`, `TextField`, `Button`):**
  * `TextArea`: Caja grande de varias líneas donde se leerá el historial del chat.
  * `TextField`: Caja pequeña de una sola línea donde escribes tu mensaje.
  * `Button`: El botón de enviar. Con `boton.setOnAction(e -> { ... })` le dices qué código ejecutar cuando alguien haga clic.

#### ✅ Tareas de la Fase 4
* [ ] **Configurar `pom.xml`:** Añadir las dependencias de `javafx-controls` y el `javafx-maven-plugin` apuntando a `com.miempresa.chat.client.ClientApp`.
* [ ] **Crear la ventana base en `ClientApp.java`:**
  * [ ] Hacer que herede de `Application` y crear el método `start(Stage primaryStage)`.
  * [ ] Ejecutar `mvn javafx:run` en la terminal de VS Code y ver aparecer tu primera ventana vacía.
* [ ] **Añadir los elementos visuales (en `ClientApp.java` o `ChatController.java`):**
  * [ ] Crear un `TextArea` (poniendo `.setEditable(false)` para que nadie borre el historial).
  * [ ] Crear un `TextField` y un `Button("Enviar")` metidos dentro de un `HBox`.
  * [ ] Juntarlo todo en un `BorderPane` y mostrarlo en la ventana.
* [ ] **Prueba de fuego:** Programar el botón para que, al pulsarlo, coja el texto del `TextField`, lo pegue en el `TextArea` y borre el `TextField`.

---

### Fase 5: Unir el Motor con la Ventana (Integración Final)

#### 🎯 ¿Por qué hacemos esta fase?
Ya tienes un motor de chat que funciona en consola (Etapa 1) y una ventana bonita que funciona en local (Fase 4). Ahora toca juntar las dos piezas para tener la aplicación definitiva.

#### 🛠️ Herramientas que vas a usar y cómo funcionan
* **`Platform.runLater(() -> { ... })` (¡La regla de oro de JavaFX!):** JavaFX es muy estricto: **prohíbe** que un hilo secundario (como el hilo de `NetworkClient` que escucha mensajes de red) modifique elementos visuales (como añadir texto al `TextArea`). Si lo haces, el programa dará un error. `Platform.runLater(...)` sirve para pasarle una tarea de forma segura al hilo principal de la ventana para que la dibuje cuando pueda.
* **`TextInputDialog`:** Una ventanita emergente ya fabricada por JavaFX que pide un dato al usuario (la usaremos al principio para preguntar tu nombre de usuario).

#### ✅ Tareas de la Fase 5
* [ ] **Pedir nombre al arrancar:** Usar `TextInputDialog` en `ClientApp` para pedir el nickname antes de abrir el chat.
* [ ] **Conectar el botón "Enviar" a la red:** Cambiar el código del botón de la Fase 4 para que llame a `networkClient.enviarMensaje(...)`.
* [ ] **Pintar mensajes entrantes:** Al llamar a `networkClient.conectar(...)`, pasarle una función que use `Platform.runLater(() -> textArea.appendText(mensaje + "\n"))`.
* [ ] **Prueba de fuego:** Arranca `Server.java` y abre 2 o 3 ventanas ejecutando `mvn javafx:run` en distintas terminales. ¡Ya tienes tu chat gráfico funcionando!