import java.util.LinkedHashMap;
import java.util.Map;

public class SimuladorAlmacen {

    // --- REPRESENTACION DEL ENTORNO ---
    private static String[][] entorno = {
            { ".", "X", ".", ".", "P" },
            { ".", "X", ".", "X", "." },
            { ".", ".", ".", ".", "." },
            { "P", "X", ".", "X", "." },
            { ".", ".", ".", ".", "P" }
    };

    // --- ESTADO DEL AGENTE Y SIMULACION ---
    private static int filaAgente = 2;
    private static int colAgente = 2;
    private static int puntuacion = 0;

    private static int totalPaquetes = 0;
    private static int paquetesRecolectados = 0;

    // Memoria interna del agente para registrar visitas
    private static int[][] visitas = new int[5][5];

    // Constantes del tablero
    private static final String OBSTACULO = "X";
    private static final String PAQUETE = "P";
    private static final String VACIA = ".";
    private static final String FUERA_TABLERO = "FUERA_DEL_TABLERO";

    // El agente no ve todo el tablero; solo percibe su celda actual y las 4
    // adyacentes.
    static class Percepcion {
        int fila;
        int col;
        String celdaActual;
        String arriba;
        String abajo;
        String izquierda;
        String derecha;

        public Percepcion(int fila, int col, String actual, String arriba, String abajo, String izquierda,
                String derecha) {
            this.fila = fila;
            this.col = col;
            this.celdaActual = actual;
            this.arriba = arriba;
            this.abajo = abajo;
            this.izquierda = izquierda;
            this.derecha = derecha;
        }
    }

    public static void main(String[] args) {
        contarPaquetesIniciales();
        visitas[filaAgente][colAgente] = 1; // Registra la celda inicial como visitada

        int paso = 0;

        // Limite de acciones
        final int MAX_ACCIONES = 50;

        System.out.println("=== INICIO DE LA SIMULACION ===");
        mostrarEntorno(paso, "INICIO");

        // Termina cuando se recogen todos los paquetes o se alcanza el limite de
        // acciones
        while (quedanPaquetes() && paso < MAX_ACCIONES) {
            paso++;

            // El agente ve su posicion y las 4 celdas adyacentes
            Percepcion percepcion = percibir();

            // El agente elige la mejor accion para hacer
            String accion = decidir(percepcion);

            // El agente ejecuta la accion elegida
            actuar(accion);

            // Muestra la posición del agente despues de cada accion
            mostrarEntorno(paso, accion);

            try {
                Thread.sleep(300); // Pausa visual para ver el avance
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n=== FIN DE LA SIMULACION ===");
        if (!quedanPaquetes()) {
            System.out.println("Todos los paquetes fueron recolectados.");
        } else {
            System.out.println("Simulacion finalizada por alcanzar el limite de " + MAX_ACCIONES + " acciones.");
        }
        System.out.println("Paquetes recolectados: " + paquetesRecolectados + " de " + totalPaquetes);
        System.out.println("Puntuacion Final Obtenida: " + puntuacion);
    }

    private static Percepcion percibir() {
        // Celda donde está parado el agente
        String actual = entorno[filaAgente][colAgente];

        // Contenido de las 4 celdas adyacentes
        String arriba = obtenerContenidoCelda(filaAgente - 1, colAgente);
        String abajo = obtenerContenidoCelda(filaAgente + 1, colAgente);
        String izquierda = obtenerContenidoCelda(filaAgente, colAgente - 1);
        String derecha = obtenerContenidoCelda(filaAgente, colAgente + 1);

        // Empaqueta todas las percepciones en un objeto para pasarlo al modulo que
        // decide
        return new Percepcion(filaAgente, colAgente, actual, arriba, abajo, izquierda, derecha);
    }

    private static String decidir(Percepcion p) {

        // Si el agente está parado sobre un paquete, la mejor accion es recogerlo
        if (p.celdaActual.equals(PAQUETE)) {
            return "RECOGER";
        }

        // Si el agente ve un paquete en alguna celda adyacente, se mueve directamente
        // hacia el
        if (p.arriba.equals(PAQUETE))
            return "ARRIBA";
        if (p.derecha.equals(PAQUETE))
            return "DERECHA";
        if (p.abajo.equals(PAQUETE))
            return "ABAJO";
        if (p.izquierda.equals(PAQUETE))
            return "IZQUIERDA";

        // No hay paquete visible; explorar el entorno.
        // Se descartan celdas invalidas
        Map<String, int[]> candidatos = new LinkedHashMap<>();
        candidatos.put("ARRIBA", new int[] { p.fila - 1, p.col, esInvalida(p.arriba) ? 1 : 0 });
        candidatos.put("DERECHA", new int[] { p.fila, p.col + 1, esInvalida(p.derecha) ? 1 : 0 });
        candidatos.put("ABAJO", new int[] { p.fila + 1, p.col, esInvalida(p.abajo) ? 1 : 0 });
        candidatos.put("IZQUIERDA", new int[] { p.fila, p.col - 1, esInvalida(p.izquierda) ? 1 : 0 });

        // Estrategia de exploracion: elegir la celda menos visitada.
        String mejorAccion = "ARRIBA";
        int menorVisitas = Integer.MAX_VALUE;

        for (Map.Entry<String, int[]> entry : candidatos.entrySet()) {
            String accion = entry.getKey();
            int r = entry.getValue()[0];
            int c = entry.getValue()[1];
            boolean esInvalido = entry.getValue()[2] == 1;

            // Solo considerar celdas validas
            if (!esInvalido) {
                int numVisitas = visitas[r][c];
                if (numVisitas < menorVisitas) {
                    menorVisitas = numVisitas;
                    mejorAccion = accion;
                }
            }
        }

        return mejorAccion;
    }

    private static boolean esInvalida(String contenido) {
        return contenido.equals(OBSTACULO) || contenido.equals(FUERA_TABLERO);
    }

    private static void actuar(String accion) {

        // Elimina el paquete de la celda actual y suma puntos.
        if (accion.equals("RECOGER")) {
            if (entorno[filaAgente][colAgente].equals(PAQUETE)) {
                entorno[filaAgente][colAgente] = VACIA;
                paquetesRecolectados++;

                actualizarRendimiento("RECOGER_PAQUETE");

                if (!quedanPaquetes()) {
                    actualizarRendimiento("TODOS_RECOLECTADOS");
                }
            }
            return;
        }

        // Calcula la nueva posición según la dirección elegida.
        int nuevaFila = filaAgente;
        int nuevaCol = colAgente;

        switch (accion) {
            case "ARRIBA":
                nuevaFila--;
                break;
            case "ABAJO":
                nuevaFila++;
                break;
            case "IZQUIERDA":
                nuevaCol--;
                break;
            case "DERECHA":
                nuevaCol++;
                break;
        }

        // Límite del tablero
        if (nuevaFila < 0 || nuevaFila >= entorno.length || nuevaCol < 0 || nuevaCol >= entorno[0].length) {
            actualizarRendimiento("FUERA_TABLERO");
        }
        // el agente no puede atravesar celdas con "X".
        else if (entorno[nuevaFila][nuevaCol].equals(OBSTACULO)) {
            actualizarRendimiento("OBSTACULO");
        } else {
            // el agente se mueve a la nueva celda válida.
            filaAgente = nuevaFila;
            colAgente = nuevaCol;
            visitas[filaAgente][colAgente]++; // Registra la visita para guiar futuras decisiones

            actualizarRendimiento("MOVIMIENTO");
        }
    }

    private static void actualizarRendimiento(String evento) {
        switch (evento) {
            case "RECOGER_PAQUETE": // Recompensa por completar el objetivo parcial
                puntuacion += 10;
                break;
            case "MOVIMIENTO": // Penalizacion leve para fomentar rutas cortas
                puntuacion -= 1;
                break;
            case "FUERA_TABLERO": // Penalizacion por accion invalida (salir del almacen)
            case "OBSTACULO": // Penalizacion por accion invalida (chocar con obstaculo)
                puntuacion -= 5;
                break;
            case "TODOS_RECOLECTADOS": // Recompensa bonus por completar la misión
                puntuacion += 20;
                break;
        }
    }

    private static String obtenerContenidoCelda(int f, int c) {
        if (f < 0 || f >= entorno.length || c < 0 || c >= entorno[0].length) {
            return FUERA_TABLERO;
        }
        return entorno[f][c];
    }

    private static boolean quedanPaquetes() {
        return paquetesRecolectados < totalPaquetes;
    }

    private static void contarPaquetesIniciales() {
        totalPaquetes = 0;
        for (int i = 0; i < entorno.length; i++) {
            for (int j = 0; j < entorno[i].length; j++) {
                if (entorno[i][j].equals(PAQUETE)) {
                    totalPaquetes++;
                }
            }
        }
    }

    // Muestra la posicion del agente despues de cada accion
    private static void mostrarEntorno(int paso, String ultimaAccion) {
        System.out.println("\n------------------------------------------------");
        System.out.println("Acción #" + paso + " | Realizada: " + ultimaAccion + " | Puntuación: " + puntuacion);
        System.out.println("Posición actual del agente (Fila, Columna): (" + filaAgente + ", " + colAgente + ")");
        System.out.println("------------------------------------------------");

        for (int i = 0; i < entorno.length; i++) {
            for (int j = 0; j < entorno[i].length; j++) {
                if (i == filaAgente && j == colAgente) {
                    System.out.print("A ");
                } else {
                    System.out.print(entorno[i][j] + " ");
                }
            }
            System.out.println();
        }
    }
}