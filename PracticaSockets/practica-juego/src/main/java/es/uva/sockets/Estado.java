package es.uva.sockets;

import java.util.ArrayList;

public class Estado {
    public final ArrayList<Jugador> jugadores;
    public Coordenadas tesoro;
    public final ArrayList<Coordenadas> buscadas;
    private boolean terminado;
    public final int size;

    public Estado(int size) {
        this.size = size;
        terminado = false;
        this.jugadores = new ArrayList<>();
        this.buscadas = new ArrayList<>();
        // TODO: Coordenadas aleatorias para el tesoro
        this.tesoro = new Coordenadas((int) (Math.random() * size), (int) (Math.random() * size));
    }

    // Los métodos que modifican el estado son synchronized,
    // Esto quiere decir que un hilo debe esperar a que otro
    // hilo acabe de utilizarlo
    // Lo necesitamos ya que vamos a gestionar cada conexión
    // con un cliente en un hilo deistinto
    public synchronized void terminar() {
        terminado = true;
    }

    public synchronized boolean estaTerminado() {
        return terminado;
    }

    public synchronized void nuevoJugador(Jugador jugador) {
        jugadores.add(jugador);
    }

    public synchronized void buscar(int id) {
        // TODO busca el tesoro el jugador con este id
        // Si se encuentra finaliza el juego
        Jugador jugador = jugadores.stream().filter(j -> j.id == id).findFirst().orElse(null); // Buscar jugador por id
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado");
        }

        Coordenadas coordenadas = jugador.coordenadas;
        if (!buscadas.contains(coordenadas)) {
            buscadas.add(coordenadas); // Añadir coordenadas a las buscadas
        }

        if (coordenadas.equals(tesoro)) {
            terminar();
        }
    }

    public synchronized void mover(int id, Direccion dir) {
        // TODO mueve a el jugador id en la direccion dir
        Jugador jugador = jugadores.stream().filter(j -> j.id == id).findFirst().orElse(null); // Buscar jugador por id
        if (jugador != null) {
            jugador.coordenadas = jugador.coordenadas.mover(dir);
        }
    }

    public void mostrar() {
        // Limpiar pantalla
        System.out.print("\033[H\033[2J");
        System.out.flush();
        // Línea horizontal
        for (int col = 0; col < size; col++) {
            System.out.print("---");
        }
        System.out.println("-");
        for (int row = 0; row < size; row++) {
            // Columnas
            for (int col = 0; col < size; col++) {
                System.out.print("| ");  // Cada celda representada por "| |"
                // Si no hay nada, imprimimos vacío
                char toPrint = ' ';
                // Si ya está cavado, imprimimos una 'x'
                for (Coordenadas coordenadas : buscadas) {
                    if (coordenadas.equals(new Coordenadas(col, row))) {
                        toPrint = 'x';
                    }
                }
                // Si hay un jugador, imprimimos su representación
                for (Jugador jugador : jugadores) {
                    if (jugador.coordenadas.equals(new Coordenadas(col, row))) {
                        toPrint = jugador.getChar();
                    }
                }
                System.out.print(toPrint);
            }
            System.out.println("|");  // Fin de la fila con un borde final

            // Imprimir separador de fila
            for (int col = 0; col < size; col++) {
                System.out.print("---");
            }
            System.out.println("-");
        }
    }
}
