# TecnoMovil Data — Módulo funcional de procesamiento de datos

Módulo en Java desarrollado con **programación funcional** (Streams, lambdas,
funciones puras y colecciones inmutables) para el caso de estudio de la
Alcaldía de TecnoValle: modernización del sistema de gestión de transporte
urbano.

## Contexto

El sistema actual procesa los datos de forma imperativa y secuencial, lo que
genera lentitud, condiciones de carrera y código difícil de mantener. Este
módulo rediseña la capa de procesamiento y agregación de datos diarios
usando un enfoque declarativo, sin efectos secundarios y compatible con
paralelización (`parallelStream()`).

## Estructura del proyecto

```
tecnomovil/
├── src/
│   ├── RegistroTransporte.java   # record inmutable: idUsuario, ruta, estacion, accion, timestamp
│   ├── RutaCritica.java          # record inmutable: resultado de detección de sobrecarga
│   ├── GeneradorDatos.java       # genera los datos simulados (única fuente de aleatoriedad)
│   ├── ProcesadorTransporte.java # núcleo funcional: funciones puras a-f
│   └── Main.java                 # demostración de ejecución
└── .gitignore
```

## Funcionalidades implementadas

| Tarea | Función | Descripción |
|---|---|---|
| a | `afluenciaPorEstacion` | Cuenta usuarios que ingresan a cada estación |
| b | `horasPico` | Agrupa eventos por hora y determina el flujo máximo |
| c | `rutasMasUtilizadas` | Ordena las rutas por volumen de uso |
| d | `patronesDeViaje` | Estaciones visitadas por usuario, en orden |
| e | `tiempoPromedioEntreEstaciones` | Tiempo promedio (min) entre eventos consecutivos por usuario |
| f | `detectarSobrecarga` | Marca como "crítica" toda ruta que supera un umbral de ocupación |

## Principios de programación funcional aplicados

- **Inmutabilidad:** todos los datos se modelan como `record`; las listas y
  mapas de resultados se devuelven con `List.copyOf` / `Collectors.toUnmodifiableList` / `Collectors.toMap` sin mutar la entrada.
- **Funciones puras:** cada método de `ProcesadorTransporte` depende solo de
  sus argumentos y no produce efectos secundarios (no hay atributos de
  instancia ni estado compartido).
- **Funciones de orden superior y lambdas:** todo el procesamiento usa
  `Stream`, `filter`, `map`, `groupingBy`, `collect` con lambdas en lugar de
  bucles imperativos con acumuladores mutables.
- **Paralelización:** al no depender de orden de ejecución ni de estado
  compartido, cualquier pipeline puede cambiar `.stream()` por
  `.parallelStream()` sin alterar el resultado (demostrado en `Main.java`).

## Cómo compilar y ejecutar

```bash
javac -d bin src/*.java
java -cp bin Main
```
