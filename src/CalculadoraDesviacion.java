import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class CalculadoraDesviacion {


    public enum TipoAnalisis {
        MUESTRA("Muestra (s)", 1),
        POBLACION("Población (o)", 0);

        private final String descripcion;
        private final int ajusteDivisor;

        TipoAnalisis(String descripcion, int ajusteDivisor) {
            this.descripcion = descripcion;
            this.ajusteDivisor = ajusteDivisor;
        }

        public String getDescripcion() { return descripcion; }
        public int getAjusteDivisor() { return ajusteDivisor; }
    }

    public record ResultadoCalculo(
            TipoAnalisis tipo,
            int cantidad,
            double media,
            double desviacionEstandar
    ){ }
    public static class ServicioEstadistico {

        public static double calcularMedia(List<Double> datos) {
            if (datos == null || datos.isEmpty()) {
                throw new IllegalArgumentException("El conjunto de datos no puede estar vacío.");
            }
            double suma = 0.0;
            for (double valor : datos) {
                suma += valor;
            }
            return suma / datos.size();
        }

        public static ResultadoCalculo procesar(List<Double> datos, TipoAnalisis tipo) {
            if (datos == null || datos.isEmpty()) {
                throw new IllegalArgumentException("El conjunto de datos no contiene elementos.");
            }

            int n = datos.size();
            // Validación de la HU-04: Evitar divisiones por cero o matemáticas indeterminadas
            if (tipo == TipoAnalisis.MUESTRA && n < 2) {
                throw new IllegalArgumentException("Para una muestra se requieren al menos 2 observaciones (divisor n - 1).");
            }
            if (tipo == TipoAnalisis.POBLACION && n < 1) {
                throw new IllegalArgumentException("Para una población se requiere al menos 1 observación.");
            }

            double media = calcularMedia(datos);
            double sumaDiferenciasCuadradas = 0.0;
            for (double valor : datos) {
                sumaDiferenciasCuadradas += Math.pow(valor - media, 2);
            }

            // Aplicación de la HU-05 y HU-06: Restar el ajusteDivisor (1 o 0) según el TipoAnalisis
            double divisor = n - tipo.getAjusteDivisor();
            double desviacionEstandar = Math.sqrt(sumaDiferenciasCuadradas / divisor);

            return new ResultadoCalculo(tipo, n, media, desviacionEstandar);
        }
    }
    public static class ServicioCsv {

        // Contenedor para devolver los datos limpios y los errores (HU-09)
        public record ResultadoLectura(List<Double> datos, List<String> errores) {
            public boolean tieneErrores() {
                return !errores.isEmpty();
            }
        }

        // HU-02: Inspeccionar las columnas disponibles en el archivo
        public static List<String> obtenerColumnas(String rutaArchivo) {
            File archivo = new File(rutaArchivo);
            // HU-01: Validar extensión
            if (!archivo.exists() || !archivo.getName().toLowerCase().endsWith(".csv")) {
                return null;
            }

            try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
                String lineaCabecera = lector.readLine();
                if (lineaCabecera == null) return null;

                String[] columnasSeparadas = lineaCabecera.split(",");
                List<String> columnas = new ArrayList<>();
                for (String col : columnasSeparadas) {
                    columnas.add(col.trim());
                }
                return columnas;
            } catch (IOException e) {
                return null;
            }
        }

        // HU-01 y HU-04: Extraer los datos y detectar errores
        public static ResultadoLectura leerColumna(String rutaArchivo, int indiceColumna) {
            List<Double> valoresValidos = new ArrayList<>();
            List<String> listaErrores = new ArrayList<>();
            File archivo = new File(rutaArchivo);

            if (!archivo.exists() || !archivo.isFile()) {
                listaErrores.add("Error: No se encontró el archivo en la ruta indicada.");
                return new ResultadoLectura(valoresValidos, listaErrores);
            }

            try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
                String linea = lector.readLine(); // Leer y saltar la cabecera
                if (linea == null) {
                    listaErrores.add("Error: El archivo CSV está vacío.");
                    return new ResultadoLectura(valoresValidos, listaErrores);
                }

                int numeroFila = 2; // Fila 1 fue la cabecera
                while ((linea = lector.readLine()) != null) {
                    if (linea.trim().isEmpty()) {
                        numeroFila++;
                        continue; // Ignorar filas completamente en blanco
                    }

                    String[] celdas = linea.split(",", -1);
                    if (indiceColumna >= celdas.length) {
                        listaErrores.add(String.format("Fila %d: La columna requerida no existe en este registro.", numeroFila));
                    } else {
                        String contenidoCelda = celdas[indiceColumna].trim();
                        // HU-04 y HU-09: Identificar celdas vacías y valores no numéricos
                        if (contenidoCelda.isEmpty()) {
                            listaErrores.add(String.format("Fila %d: Se detectó una celda vacía.", numeroFila));
                        } else {
                            try {
                                valoresValidos.add(Double.parseDouble(contenidoCelda));
                            } catch (NumberFormatException e) {
                                listaErrores.add(String.format("Fila %d: El valor '%s' no es numérico.", numeroFila, contenidoCelda));
                            }
                        }
                    }
                    numeroFila++;
                }
            } catch (IOException e) {
                listaErrores.add("Error de lectura: " + e.getMessage());
            }

            // Validar que al menos haya un dato procesable
            if (listaErrores.isEmpty() && valoresValidos.isEmpty()) {
                listaErrores.add("Error: La columna elegida no contiene registros numéricos.");
            }

            return new ResultadoLectura(valoresValidos, listaErrores);
        }
    }
    public static class SesionDatos {
        private List<Double> datosActuales = new ArrayList<>();
        private String descripcionOrigen = "";
        private boolean cargado = false;

        // HU-10: Limpiar datos actuales y comenzar de nuevo
        public void reiniciar() {
            this.datosActuales.clear();
            this.descripcionOrigen = "";
            this.cargado = false;
        }

        public boolean estaCargado() {
            return cargado;
        }

        public List<Double> getDatosActuales() {
            return new ArrayList<>(datosActuales); // Devuelve una copia para evitar modificaciones accidentales
        }

        // HU-03 y HU-04: Captura manual de valores
        public List<String> cargarValoresManuales(String entradaTexto) {
            List<String> errores = new ArrayList<>();
            // Reemplaza comas por espacios y separa por cualquier cantidad de espacios en blanco
            String[] fragmentos = entradaTexto.replace(",", " ").trim().split("\\s+");

            if (fragmentos.length == 0 || (fragmentos.length == 1 && fragmentos[0].isEmpty())) {
                errores.add("Error: Debe ingresar al menos un valor numérico.");
                return errores;
            }

            List<Double> valoresProcesados = new ArrayList<>();
            for (int i = 0; i < fragmentos.length; i++) {
                String token = fragmentos[i];
                try {
                    valoresProcesados.add(Double.parseDouble(token));
                } catch (NumberFormatException e) {
                    errores.add(String.format("Valor #%d ('%s') no es un número válido.", (i + 1), token));
                }
            }

            if (!errores.isEmpty()) {
                return errores;
            }

            this.datosActuales = valoresProcesados;
            this.descripcionOrigen = String.format("Captura manual (%d observaciones)", this.datosActuales.size());
            this.cargado = true;
            return errores;
        }

        // HU-01 y HU-02: Puente entre el CSV y la Sesión
        public List<String> cargarDesdeCsv(String rutaArchivo, int indiceColumna, String nombreColumna) {
            ServicioCsv.ResultadoLectura resultado = ServicioCsv.leerColumna(rutaArchivo, indiceColumna);

            if (resultado.tieneErrores()) {
                return resultado.errores();
            }

            this.datosActuales = resultado.datos();
            this.descripcionOrigen = String.format("Archivo CSV '%s' -> Columna: '%s'", rutaArchivo, nombreColumna);
            this.cargado = true;
            return resultado.errores();
        }

        // HU-08: Visualizar los datos antes del cálculo
        public void mostrarVistaPrevia(int limiteElementos) {
            if (!cargado || datosActuales.isEmpty()) {
                System.out.println("\n[!] No hay datos cargados en la sesión actual.");
                return;
            }

            System.out.println("\n--- VISTA PREVIA DE DATOS (HU-08) ---");
            System.out.println("Origen: " + descripcionOrigen);
            System.out.println("Total de observaciones: " + datosActuales.size());

            StringBuilder vista = new StringBuilder();
            int maximo = Math.min(datosActuales.size(), limiteElementos);
            for (int i = 0; i < maximo; i++) {
                vista.append(datosActuales.get(i));
                if (i < maximo - 1) vista.append(", ");
            }
            if (datosActuales.size() > limiteElementos) {
                vista.append("..."); // Indica que hay más datos ocultos
            }
            System.out.println("Valores registrados: [" + vista + "]");
            System.out.println("-------------------------------------");
        }
    }
    public static void main(String[] args) {
        Scanner entradaTeclado = new Scanner(System.in);
        SesionDatos sesion = new SesionDatos();
        boolean ejecutando = true;

        while (ejecutando) {
            System.out.println("\n----------------------------------------------");
            System.out.println("    CALCULADORA DE DESVIACIÓN ESTÁNDAR    ");
            System.out.println("----------------------------------------------");
            System.out.println("1. Cargar archivo CSV (HU-01, HU-02)");
            System.out.println("2. Capturar valores manualmente (HU-03)");
            System.out.println("3. Vista previa de datos cargados (HU-08)");
            System.out.println("4. Calcular desviación estándar (HU-05, HU-06, HU-07)");
            System.out.println("5. Reiniciar sesión / Limpiar datos (HU-10)");
            System.out.println("6. Salir");
            System.out.print("\nSeleccione una opción (1-6): ");

            String opcion = entradaTeclado.nextLine().trim();

            switch (opcion) {
                case "1" -> {
                    System.out.print("\nIngrese la ruta del archivo .csv: ");
                    String ruta = entradaTeclado.nextLine().trim();

                    if (!ruta.toLowerCase().endsWith(".csv")) {
                        System.out.println("[!] Error: El archivo debe tener extensión .csv (HU-01).");
                        break;
                    }

                    List<String> columnas = ServicioCsv.obtenerColumnas(ruta);
                    if (columnas == null || columnas.isEmpty()) {
                        System.out.println("[!] Error: No se pudo leer el archivo o el CSV no tiene cabeceras.");
                        break;
                    }

                    System.out.println("\nColumnas disponibles en el archivo:");
                    for (int i = 0; i < columnas.size(); i++) {
                        System.out.printf("  %d. %s\n", (i + 1), columnas.get(i));
                    }

                    System.out.print("\nIngrese el número o nombre de la columna: ");
                    String seleccion = entradaTeclado.nextLine().trim();
                    int indiceSeleccionado = -1;
                    String nombreColumna = "";

                    if (seleccion.matches("\\d+")) {
                        int indiceNumerico = Integer.parseInt(seleccion) - 1;
                        if (indiceNumerico >= 0 && indiceNumerico < columnas.size()) {
                            indiceSeleccionado = indiceNumerico;
                            nombreColumna = columnas.get(indiceSeleccionado);
                        }
                    } else {
                        for (int i = 0; i < columnas.size(); i++) {
                            if (columnas.get(i).equalsIgnoreCase(seleccion)) {
                                indiceSeleccionado = i;
                                nombreColumna = columnas.get(i);
                                break;
                            }
                        }
                    }

                    if (indiceSeleccionado == -1) {
                        System.out.println("[!] Columna inválida o inexistente.");
                        break;
                    }

                    List<String> fallosCsv = sesion.cargarDesdeCsv(ruta, indiceSeleccionado, nombreColumna);
                    if (!fallosCsv.isEmpty()) {
                        System.out.println("\n[!] Falló la validación del CSV (HU-04, HU-09):");
                        int tope = Math.min(fallosCsv.size(), 10);
                        for (int i = 0; i < tope; i++) {
                            System.out.println("  - " + fallosCsv.get(i));
                        }
                        if (fallosCsv.size() > 10) {
                            System.out.printf("  ... y %d errores adicionales no mostrados.\n", fallosCsv.size() - 10);
                        }
                    } else {
                        System.out.printf("\n[✓] Datos cargados correctamente de '%s'.\n", nombreColumna);
                        sesion.mostrarVistaPrevia(10);
                    }
                }
                case "2" -> {
                    System.out.println("\nIngrese los números separados por comas o espacios:");
                    System.out.print("> ");
                    String entradaManual = entradaTeclado.nextLine();
                    List<String> fallosManuales = sesion.cargarValoresManuales(entradaManual);

                    if (!fallosManuales.isEmpty()) {
                        System.out.println("\n[!] Error en los datos introducidos (HU-04, HU-09):");
                        for (String fallo : fallosManuales) {
                            System.out.println("  - " + fallo);
                        }
                    } else {
                        System.out.println("\n[✓] Datos capturados correctamente.");
                        sesion.mostrarVistaPrevia(10);
                    }
                }
                case "3" -> sesion.mostrarVistaPrevia(15);
                case "4" -> {
                    if (!sesion.estaCargado() || sesion.getDatosActuales().isEmpty()) {
                        System.out.println("\n[!] No hay datos cargados. Capture datos o cargue un CSV primero.");
                        break;
                    }

                    sesion.mostrarVistaPrevia(5);

                    System.out.println("\nSeleccione la fórmula a aplicar (HU-05):");
                    System.out.println("1. Muestra   [Divisor: n - 1]");
                    System.out.println("2. Población [Divisor: N]");
                    System.out.print("Opción (1/2, predeterminada 1): ");
                    String seleccionTipo = entradaTeclado.nextLine().trim();
                    TipoAnalisis tipo = seleccionTipo.equals("2") ? TipoAnalisis.POBLACION : TipoAnalisis.MUESTRA;

                    try {
                        ResultadoCalculo resultado = ServicioEstadistico.procesar(sesion.getDatosActuales(), tipo);

                        System.out.println("\n============= RESULTADOS FINALES (HU-06, HU-07) =============");
                        System.out.printf("Tipo aplicado:          %s\n", resultado.tipo().getDescripcion());
                        System.out.printf("Observaciones (n / N):  %d\n", resultado.cantidad());
                        System.out.printf("Media aritmética (x̄/μ): %.6f\n", resultado.media());
                        System.out.printf("Desviación estándar:    %.6f\n", resultado.desviacionEstandar());
                        System.out.println("=============================================================");
                    } catch (IllegalArgumentException e) {
                        System.out.printf("\n[!] Error en validación de cálculo (HU-04): %s\n", e.getMessage());
                    }
                }
                case "5" -> {
                    sesion.reiniciar();
                    System.out.println("\n[✓] Sesión limpiada: se borraron los datos en memoria y cálculos anteriores (HU-10).");
                }
                case "6" -> {
                    ejecutando = false;
                    System.out.println("\nFinalizando la aplicación.");
                }
                default -> System.out.println("\n[!] Opción no reconocida. Intente de nuevo.");
            }
        }
        entradaTeclado.close()
    }
}
} //fin
