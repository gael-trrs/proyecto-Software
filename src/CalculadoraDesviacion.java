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
    ){

    }
}
