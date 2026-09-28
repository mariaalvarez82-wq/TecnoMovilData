import java.util.List;
import java.util.Map;

/**
 * Punto de entrada que demuestra el procesamiento funcional de los datos
 * simulados de TecnoMovil Data.
 */
public class Main {
    public static void main(String[] args) {

        // 1. Generación de datos simulados (200 usuarios, semilla fija = 2026
        //    para que la ejecución sea reproducible en cualquier máquina)
        List<RegistroTransporte> registros = GeneradorDatos.generar(200, 2026L);
        System.out.println("Total de registros generados: " + registros.size());
        System.out.println("===========================================================");

        // a) Afluencia por estación
        System.out.println("\na) AFLUENCIA POR ESTACION (usuarios que ingresaron)");
        Map<String, Long> afluencia = ProcesadorTransporte.afluenciaPorEstacion(registros);
        afluencia.forEach((estacion, total) -> System.out.println("   " + estacion + ": " + total));

        // b) Horas pico
        System.out.println("\nb) HORAS PICO (eventos por hora, de mayor a menor)");
        Map<Integer, Long> horasPico = ProcesadorTransporte.horasPico(registros);
        horasPico.forEach((hora, total) -> System.out.println("   " + hora + ":00h -> " + total + " eventos"));

        // c) Rutas más utilizadas
        System.out.println("\nc) RUTAS MAS UTILIZADAS");
        Map<String, Long> rutas = ProcesadorTransporte.rutasMasUtilizadas(registros);
        rutas.forEach((ruta, total) -> System.out.println("   " + ruta + ": " + total + " eventos"));

        // d) Patrones de viaje por usuario (se muestran solo los primeros 5 usuarios)
        System.out.println("\nd) PATRONES DE VIAJE POR USUARIO (muestra de 5 usuarios)");
        Map<String, List<String>> patrones = ProcesadorTransporte.patronesDeViaje(registros);
        patrones.entrySet().stream()
                .limit(5)
                .forEach(e -> System.out.println("   " + e.getKey() + " -> " + e.getValue()));

        // e) Tiempo promedio entre estaciones (se muestran solo los primeros 5 usuarios)
        System.out.println("\ne) TIEMPO PROMEDIO ENTRE ESTACIONES (minutos, muestra de 5 usuarios)");
        Map<String, Double> tiempos = ProcesadorTransporte.tiempoPromedioEntreEstaciones(registros);
        tiempos.entrySet().stream()
                .limit(5)
                .forEach(e -> System.out.printf("   %s -> %.2f min%n", e.getKey(), e.getValue()));

        // f) Detección de sobrecarga en rutas (umbral simulado: 70 pasajeros/dia)
        System.out.println("\nf) DETECCION DE SOBRECARGA EN RUTAS (umbral = 70 pasajeros)");
        List<RutaCritica> sobrecarga = ProcesadorTransporte.detectarSobrecarga(registros, 70);
        sobrecarga.forEach(rc -> System.out.println("   " + rc.ruta() + ": " + rc.totalPasajeros()
                + " pasajeros -> " + (rc.critica() ? "CRITICA" : "normal")));

        // Ejemplo de paralelización: mismo cálculo de afluencia usando parallelStream()
        System.out.println("\n--- Ejemplo con parallelStream() (mismo resultado que a) ---");
        Map<String, Long> afluenciaParalela = registros.parallelStream()
                .filter(r -> r.accion().equals("entrada"))
                .collect(java.util.stream.Collectors.groupingBy(
                        RegistroTransporte::estacion,
                        java.util.stream.Collectors.counting()
                ));
        System.out.println("   Resultado identico al secuencial: " + afluenciaParalela.equals(afluencia));
    }
}
