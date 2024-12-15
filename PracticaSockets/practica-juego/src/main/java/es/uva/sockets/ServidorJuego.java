package es.uva.sockets;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServidorJuego {
    // El juego consiste en encontrar un tesoro
    // en un mapa cuadriculado, cuando un jugador
    // se conecta aparece en un cuadrado aleatorio
    // no ocupado.
    // El _PROTOCOLO_ del cliente, la manera en que
    // se comunica con el servidor es el siguiente
    // MOVE UP|DOWN|LEFT|RIGHT
    // DIG
    // EL Servidor Verifica la validez de los movimientos
    // Los aplica sobre su estado y envía la actualización
    // A todos los jugadores.
    // EL _PROTOCOLO_ con el que el servidor comunica las
    // actualizaciones a los clientes es el siguiente
    // PLAYER JOIN <PLAYER-ID> <X> <Y>
    // MOVE UP|DOWN|LEFT|RIGHT <PLAYER-ID>
    // DIG <PLAYER-ID> <SUCCESS>
    // El delimitador de lo que constituye un mensaje es
    // un caracter de salto de linea
    public final Estado estado;
    public final ServerSocket serverSocket;
    private final List<ManagerCliente> clientes;

    public ServidorJuego(int size, int puerto) throws IOException {
        estado = new Estado(size);
        clientes = new ArrayList<>();
        // Crear un serverSocket que acepte
        // conexiones de VARIOS clientes
        serverSocket = new ServerSocket(puerto);
        System.out.println("Servidor inicializado en: " + puerto);
    }

    public void iniciar() throws IOException {
        while (!estado.estaTerminado()) {
            ManagerCliente nuevo = aceptarConexion();
            clientes.add(nuevo);
            nuevo.start();
        }
    }

    public ManagerCliente aceptarConexion() throws IOException {
        // TODO: Usando el serverSocket
        // Al añadir un nuevo jugador se le deben enviar
        // la posicion de los jugadores existentes, aunque no
        // sabe donde han estado buscando.
        Socket socket = serverSocket.accept();
        System.out.println("Socket aceptado: " + socket);

        Jugador jugador = crearJugador();
        ManagerCliente managerCliente = configurarCliente(socket, jugador);

        enviarMensajeInicial(managerCliente, jugador);
        broadcastJugadorConectado(jugador);

        return managerCliente;
    }

    // Método que se encarga de crear un nuevo jugador y añadirlo al estado
    private Jugador crearJugador() {
        int idJugador = estado.jugadores.size() + 1;
        Coordenadas coordenadas = new Coordenadas(0, 0);
        Jugador jugador = new Jugador(idJugador, coordenadas);
        estado.nuevoJugador(jugador);
        System.out.println("Jugador creado: Id=" + idJugador + ", Coordenadas=" + coordenadas);
        return jugador;
    }

    // Método que se encarga de configurar un nuevo cliente y añadirlo a la lista de clientes
    private ManagerCliente configurarCliente(Socket socket, Jugador jugador) throws IOException {
        ManagerCliente managerCliente = new ManagerCliente(socket, this, jugador.id);
        clientes.add(managerCliente);
        return managerCliente;
    }

    // Método que se encarga de enviar un mensaje a todos los clientes informando de la conexión de un nuevo jugador
    private void broadcastJugadorConectado(Jugador jugador) {
        broadcast("PLAYER JOIN " + jugador.id + " " + jugador.coordenadas.getX() + " " + jugador.coordenadas.getY());
    }


    // Método que se encarga de enviar un mensaje al cliente con la posición de los jugadores existentes
    private void enviarMensajeInicial(ManagerCliente managerCliente, Jugador jugador) {
        for (Jugador j : estado.jugadores) {
            if (j.id != jugador.id) {
                managerCliente.enviarMensaje("PLAYER JOIN " + j.id + " " + j.coordenadas.getX() + " " + j.coordenadas.getY());
            }
        }
        System.out.println("Mensaje enviado al jugador: " + jugador.id);
    }


    public synchronized void broadcast(String message) {
        // TODO: Enviar un mensaje a todos los clientes
        for (ManagerCliente cliente : clientes) {
            cliente.enviarMensaje(message);
        }
        System.out.println("Broadcast enviado: " + message);
    }
}
