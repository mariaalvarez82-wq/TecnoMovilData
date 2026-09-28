import java.time.LocalDateTime;

/**
 * Registro inmutable de un evento de transporte (entrada o salida de un
 * usuario en una estación). Al ser un record, todos sus campos son finales
 * y no existen setters: cualquier "modificación" produce un nuevo objeto,
 * lo que garantiza inmutabilidad en todo el pipeline funcional.
 */
public record RegistroTransporte(
        String idUsuario,
        String ruta,
        String estacion,
        String accion,      // "entrada" o "salida"
        LocalDateTime timestamp
) {
}
