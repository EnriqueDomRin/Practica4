package es.uva.sockets;

import java.io.*;
import java.net.Socket;

public class ClienteJuego {
    // La clase cliente tiene las siguientes responsabilidades
    // Unirse al juego conectandose al servidor
    // Mantener un estado de juego actualizado interpretando los
    // mensajes del servidor (y mostrar el estado)
    // Convertir input del jugador en un mensaje que enviar al servidor
    // NOTA: para simplificar el manejo de input podemos considerar
    // que el usario manda cada comando en una linea distinta
    // (aunque sea muy incomodo)
    public final Estado estado;

    // TODO: Faltarán atributos ...
    private BufferedReader entrada; // Para leer mensajes del servidor
    private PrintWriter salida; // Para enviar mensajes al servidor
    private Socket socket; // Para comunicarse con el servidor


    public ClienteJuego(int size) {
        // [OPCIONAL] TODO: Extiende el protocolo de comunicacion para
        // que el servidor envie el tamaño del mapa tras la conexion
        // de manera que el estado no se instancie hasta entonces
        // y conocer este parametro a priori no sea necesario.
        estado = new Estado(size);
    }

    public void iniciar(String host, int puerto) throws InterruptedException {
        // Metodo que reune todo y mantiene lo necesario en un bucle
        conectar(host, puerto);
        Thread procesadorMensajesServidor = new Thread(() -> {
            while (!estado.estaTerminado()) {
                procesarMensajeServidor();
            }
        });
        Thread procesadorInput = new Thread(() -> {
            while (!estado.estaTerminado()) {
                procesarInput();
            }
        });
        procesadorMensajesServidor.start();
        procesadorInput.start();
        procesadorInput.join();
        procesadorMensajesServidor.join();
        // Si acaban los hilos es que el juego terminó
        cerrarConexion();
    }

    public void cerrarConexion() {
        // TODO: cierra todos los recursos asociados a la conexion con el servidor
        try {
            if (entrada != null) entrada.close(); // Cerrar el buffer de lectura
            if (salida != null) salida.close(); // Cerrar el buffer de escritura
            if (socket != null) socket.close(); // Cerrar el socket
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void conectar(String host, int puerto) {
        // TODO: iniciar la conexion con el servidor
        // (Debe guardar la conexion en un atributo)
        try {
            socket = new Socket(host, puerto); // para conectarse al servidor
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream())); // para leer mensajes del servidor
            salida = new PrintWriter(socket.getOutputStream(), true); // para enviar mensajes al servidor
            System.out.println("Cliente conectado en " + host + ":" + puerto); // para informar al usuario
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void procesarInput() {
        // TODO: Comprueba la entrada estandar y
        // se procesa mediante intrepretar input,
        // Se genera un mensaje que se envia al servidor
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Introduce un comando: ");
            String input = in.readLine();

            // Si el input no está vacío, se envía al servidor
            if (input != null && !input.isEmpty()) {
                String comando = interpretarInput(input.charAt(0));
                salida.println(comando);
                System.out.println("Comando enviado al servidor: " + comando);
            // Si el input está vacío, se informa al usuario
            } else {
                System.out.println("Entrada vacía, no se envió ningún comando.");
            }
        } catch (IOException e) {
            System.err.println("Error procesando la entrada del jugador.");
            e.printStackTrace();
        }
    }


    public void procesarMensajeServidor() {
        // TODO: Comprueba la conexion y obtiene un mensaje
        // que se procesa con interpretarMensaje
        // Al recibir la actualizacion del servidor podeis
        // Usar el metodo mostrar del estado
        // Para enseñarlo
        try {
            if (entrada.ready()) {
                String mensaje = entrada.readLine();
                if (mensaje != null) {
                    System.out.println("Mensaje recibido del servidor: " + mensaje);
                    interpretarMensaje(mensaje);
                    estado.mostrar();
                } else {
                    System.out.println("No se ha recibido ningún mensaje.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String interpretarInput(char tecla) {
        // TODO: WASD para moverse, Q para buscar
        // Este metodo debe devolver el comando necesario
        // Que enviar al servidor
        return switch (tecla) {
            case 'W' -> "MOVE UP";
            case 'S' -> "MOVE DOWN";
            case 'A' -> "MOVE LEFT";
            case 'D' -> "MOVE RIGHT";
            case 'Q' -> "DIG";
            default -> "";
        };
    }

    public void interpretarMensaje(String mensaje) {
        // TODO: interpretar los mensajes del servidor actualizando el estado
        String[] opciones = mensaje.split(" ");
        switch (opciones[0]) {

            case "PLAYER" -> { // caso para añadir un nuevo jugador
                if (opciones[1].equals("JOIN")) {
                    int id = Integer.parseInt(opciones[2]);
                    int x = Integer.parseInt(opciones[3]);
                    int y = Integer.parseInt(opciones[4]);
                    estado.nuevoJugador(new Jugador(id, new Coordenadas(x, y)));
                }
            }
            case "MOVE" -> { // caso para mover un jugador
                int id = Integer.parseInt(opciones[2]);
                Direccion dir = Direccion.valueOf(opciones[1]);
                estado.mover(id, dir);
            }
            case "DIG" -> { // caso para buscar un tesoro
                int id = Integer.parseInt(opciones[1]);
                boolean success = Boolean.parseBoolean(opciones[2]);
                Jugador jugador = estado.jugadores.stream()
                        .filter(j -> j.id == id)
                        .findFirst()
                        .orElse(null);
                if (jugador != null) {
                    estado.buscadas.add(jugador.coordenadas);
                    System.out.println("Coordenada añadida: " + jugador.coordenadas.getX() + "," + jugador.coordenadas.getY());
                }
                if (success) {
                    estado.terminar();
                    System.out.println("El tesoro ha sido encontrado por el Jugador: " + id);
                }
            }
        }
    }
}
