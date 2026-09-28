import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Módulo funcional de procesamiento y agregación de datos de TecnoMovil Data.
 *
 * Todas las funciones de esta clase son PURAS:
 *   - No modifican la lista de entrada (nunca se hace add/remove/set sobre
 *     los registros recibidos; List.copyOf y Collectors.toUnmodifiableList/Map
 *     se usan para blindar los resultados).
 *   - No leen ni escriben variables externas (no hay estado compartido ni
 *     atributos mutables en esta clase; es efectivamente estática y sin
 *     efectos secundarios).
 *   - Para la misma lista de entrada, siempre producen la misma salida.
 *
 * Todas se implementan como pipelines declarativos de Stream (filter, map,
 * groupingBy, collect...) en lugar de bucles imperativos con acumuladores
 * mutables, y todas son candidatas directas a paralelizarse cambiando
 * .stream() por .parallelStream(), porque no dependen de orden de ejecución
 * ni de estado compartido entre iteraciones.
 */
public class ProcesadorTransporte {

    private ProcesadorTransporte() {
        // Clase de solo funciones estáticas; no se instancia.
    }

    // -----------------------------------------------------------------
    // a) Cálculo de afluencia por estación
    // -----------------------------------------------------------------
    /**
     * Cuenta cuántos usuarios ingresan (accion = "entrada") a cada estación.
     */
    public static Map<String, Long> afluenciaPorEstacion(List<RegistroTransporte> registros) {
        return registros.stream()
                .filter(r -> r.accion().equals("entrada"))
                .collect(Collectors.groupingBy(
                        RegistroTransporte::estacion,
                        Collectors.counting()
                ));
    }

    // -----------------------------------------------------------------
    // b) Identificación de horas pico
    // -----------------------------------------------------------------
    /**
     * Agrupa todos los registros por la hora del día (0-23) en la que
     * ocurrieron y cuenta cuántos eventos hay en cada hora. El resultado
     * viene ordenado de mayor a menor flujo (LinkedHashMap preserva orden).
     */
    public static Map<Integer, Long> horasPico(List<RegistroTransporte> registros) {
        Map<Integer, Long> conteoPorHora = registros.stream()
                .collect(Collectors.groupingBy(
                        r -> r.timestamp().getHour(),
                        Collectors.counting()
                ));

        return conteoPorHora.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    // -----------------------------------------------------------------
    // c) Rutas más utilizadas
    // -----------------------------------------------------------------
    /**
     * Determina el volumen de uso de cada ruta (número total de eventos
     * registrados), ordenado de mayor a menor.
     */
    public static Map<String, Long> rutasMasUtilizadas(List<RegistroTransporte> registros) {
        Map<String, Long> conteoPorRuta = registros.stream()
                .collect(Collectors.groupingBy(
                        RegistroTransporte::ruta,
                        Collectors.counting()
                ));

        return conteoPorRuta.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    // -----------------------------------------------------------------
    // d) Patrones de viaje por usuario
    // -----------------------------------------------------------------
    /**
     * Para cada usuario, genera la lista de estaciones visitadas en el
     * orden en que aparecen los registros (según timestamp).
     */
    public static Map<String, List<String>> patronesDeViaje(List<RegistroTransporte> registros) {
        return registros.stream()
                .filter(r -> r.accion().equals("entrada"))
                .collect(Collectors.groupingBy(
                        RegistroTransporte::idUsuario,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                lista -> lista.stream()
                                        .sorted(Comparator.comparing(RegistroTransporte::timestamp))
                                        .map(RegistroTransporte::estacion)
                                        .collect(Collectors.toUnmodifiableList())
                        )
                ));
    }

    // -----------------------------------------------------------------
    // e) Cálculo de tiempo promedio entre estaciones
    // -----------------------------------------------------------------
    /**
     * Para cada usuario, calcula el tiempo promedio (en minutos) entre
     * eventos consecutivos de "entrada", es decir, el tiempo típico que
     * transcurre entre que el usuario aparece en una estación y en la
     * siguiente.
     */
    public static Map<String, Double> tiempoPromedioEntreEstaciones(List<RegistroTransporte> registros) {
        Map<String, List<LocalDateTime>> timestampsPorUsuario = registros.stream()
                .filter(r -> r.accion().equals("entrada"))
                .collect(Collectors.groupingBy(
                        RegistroTransporte::idUsuario,
                        Collectors.mapping(
                                RegistroTransporte::timestamp,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        lista -> lista.stream()
                                                .sorted()
                                                .collect(Collectors.toUnmodifiableList())
                                )
                        )
                ));

        return timestampsPorUsuario.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> promedioMinutosEntreConsecutivos(entry.getValue())
                ));
    }

    /**
     * Función pura auxiliar: dada una lista ordenada de timestamps,
     * calcula el promedio (en minutos) de la diferencia entre cada par
     * de elementos consecutivos.
     */
    private static double promedioMinutosEntreConsecutivos(List<LocalDateTime> timestamps) {
        return java.util.stream.IntStream.range(1, timestamps.size())
                .mapToLong(i -> Duration.between(timestamps.get(i - 1), timestamps.get(i)).toMinutes())
                .average()
                .orElse(0.0);
    }

    // -----------------------------------------------------------------
    // f) Detección de sobrecarga en rutas
    // -----------------------------------------------------------------
    /**
     * Marca como "crítica" cada ruta cuyo total de pasajeros (eventos de
     * "entrada") supera el umbral indicado.
     */
    public static List<RutaCritica> detectarSobrecarga(List<RegistroTransporte> registros, long umbral) {
        Map<String, Long> pasajerosPorRuta = registros.stream()
                .filter(r -> r.accion().equals("entrada"))
                .collect(Collectors.groupingBy(
                        RegistroTransporte::ruta,
                        Collectors.counting()
                ));

        return pasajerosPorRuta.entrySet().stream()
                .map(entry -> new RutaCritica(entry.getKey(), entry.getValue(), entry.getValue() > umbral))
                .sorted(Comparator.comparingLong(RutaCritica::totalPasajeros).reversed())
                .collect(Collectors.toUnmodifiableList());
    }
}
