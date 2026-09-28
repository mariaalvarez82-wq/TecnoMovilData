/**
 * Resultado del análisis de sobrecarga de una ruta. Es un record (inmutable)
 * producido por una función pura: dado el mismo total de pasajeros y el
 * mismo umbral, "critica" siempre será el mismo valor.
 */
public record RutaCritica(String ruta, long totalPasajeros, boolean critica) {
}
