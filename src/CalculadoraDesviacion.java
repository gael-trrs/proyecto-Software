import java.util.List;



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
}
