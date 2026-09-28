import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Genera el conjunto de datos simulados que representa los registros
 * capturados por validadores, sensores y apps móviles.
 *
 * Esta clase es la ÚNICA fuente de aleatoriedad de todo el módulo. Se
 * aísla intencionalmente aquí porque generar números aleatorios es, por
 * definición, un efecto impuro (misma entrada -> distinta salida). Al
 * concentrarlo en un solo punto, el resto del sistema (ProcesadorTransporte)
 * puede mantenerse 100% compuesto por funciones puras que operan sobre
 * la lista ya generada.
 */
public class GeneradorDatos {

    private static final String[] RUTAS = {"R101", "R202", "R303", "R404", "R505"};
    private static final String[] ESTACIONES = {
            "Estacion Central", "Portal Norte", "Portal Sur", "El Prado", "Centro Historico"
    };

    /**
     * Genera {@code cantidadUsuarios} usuarios con varios registros de
     * entrada/salida cada uno, durante un día simulado, y devuelve la
     * lista como inmutable (List.copyOf) para reforzar que ningún
     * consumidor puede alterarla.
     */
    public static List<RegistroTransporte> generar(int cantidadUsuarios, long semilla) {
        Random random = new Random(semilla);
        List<RegistroTransporte> registros = new ArrayList<>();

        for (int u = 1; u <= cantidadUsuarios; u++) {
            String idUsuario = String.format("U%04d", u);
            int viajes = 2 + random.nextInt(4); // entre 2 y 5 viajes por usuario
            int horaBase = 5; // el día simulado inicia a las 5:00 am

            for (int v = 0; v < viajes; v++) {
                String ruta = RUTAS[random.nextInt(RUTAS.length)];
                String estacion = ESTACIONES[random.nextInt(ESTACIONES.length)];
                int hora = Math.min(23, horaBase + random.nextInt(3));
                int minuto = random.nextInt(60);
                LocalDateTime tEntrada = LocalDateTime.of(2026, 9, 27, hora, minuto);

                registros.add(new RegistroTransporte(idUsuario, ruta, estacion, "entrada", tEntrada));

                LocalDateTime tSalida = tEntrada.plusMinutes(5 + random.nextInt(40));
                registros.add(new RegistroTransporte(idUsuario, ruta, estacion, "salida", tSalida));

                horaBase = Math.min(22, hora + 1);
            }
        }
        return List.copyOf(registros);
    }
}
