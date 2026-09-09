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
}
