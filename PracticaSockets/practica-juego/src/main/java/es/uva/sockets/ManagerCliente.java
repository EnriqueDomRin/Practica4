package es.uva.sockets;

import java.io.*;
import java.net.Socket;

public class ManagerCliente extends Thread {
    // Clase para que el encargado de cada cliente
    // Se ejecute en un hilo diferente

    private final Socket socket;
    private final ServidorJuego servidor;
    private final int idJugador;

    // Se pueden usar mas atributos ...
    private BufferedReader entrada; // Para leer mensajes del cliente
    private PrintWriter salida; // Para enviar mensajes al cliente

    public ManagerCliente(Socket socket, ServidorJuego servidor, int idJugador) {
        this.socket = socket;
        this.servidor = servidor;
        this.idJugador = idJugador;
        // Se pueden usar mas atributos ...
        // Inicializar los buffers de lectura y escritura del socket
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("ManagerCliente creado para el jugador: " + idJugador);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void enviarMensaje(String message) {
        // TODO: enviar un mensaje. NOTA: a veces hace falta usar flush.
        if (!socket.isClosed()) {
            salida.println(message);
            salida.flush();
        }
    }

    public void procesarMensajeCliente() {
        // TODO: leer el mensaje del cliente
        // y procesarlo usando interpretarMensaje
        // Si detectamos el final del socket
        // gestionar desconexion ...
        try {
            if (entrada.ready()) {
                String mensaje = entrada.readLine();
                if (mensaje != null) {
                    interpretarMensaje(mensaje);
                } else {
                    cerrarConexion();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método que se encarga de cerrar la conexión con el cliente
    private void cerrarConexion() throws IOException {
        socket.close();
        System.out.println("Socket cerrado para el jugador: " + idJugador);
    }

    @Override
    public void run() {
        // Mantener todos los procesos necesarios hasta el final
        // de la partida (alguien encuentra el tesoro)
        while (!servidor.estado.estaTerminado() && !socket.isClosed()) {
            procesarMensajeCliente();
        }
    }

    public void interpretarMensaje(String mensaje) {
        // TODO: Esta función debe realizar distintas
        // Acciones según el mensaje recibido
        // Manipulando el estado del servidor
        // Si el mensaje recibido no tiene el formato correcto
        // No ocurre nada
        String[] opciones = mensaje.split(" ");
        try {
            switch (opciones[0]) {
                case "MOVE":
                    Direccion dir = Direccion.valueOf(opciones[1]);
                    servidor.estado.mover(idJugador, dir);
                    servidor.broadcast("MOVE " + opciones[1] + " " + idJugador);
                    break;
                case "DIG":
                    servidor.estado.buscar(idJugador);
                    servidor.broadcast("DIG " + idJugador + " " + servidor.estado.estaTerminado());
                    break;
                default:
                    System.out.println("Error en el comando del cliente " + idJugador + ": " + mensaje);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
