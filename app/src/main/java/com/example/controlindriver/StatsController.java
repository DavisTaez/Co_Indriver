package com.example.controlindriver;
import android.content.Context;
import java.util.List;

public class StatsController {
    public static class Resumen {
        public double gananciasDelDia;
        public double combustible; // <-- este es el que te marcaba en rojo
        public double comision;
        public double gananciaNeta;
        public double promedio;
        public double totalPropinas;
        public int kmRecorridos;
        public int carrerasRealizadas;
    }

    public Resumen calcular(Context ctx, List<Carrera> lista) {
        Configuracion config = new Configuracion(ctx);
        Resumen r = new Resumen();
        for (Carrera c : lista) {
            if (c.cancelada) continue;
            r.carrerasRealizadas++;
            r.gananciasDelDia += c.valor;
            r.totalPropinas += c.propina;
            r.kmRecorridos += c.getKmRecorridos();
        }
        double comisionPct = config.getComisionPorcentaje() / 100.0;
        double precioGalon = config.getPrecioGalon();
        double rendimiento = config.getRendimiento();

        r.combustible = rendimiento > 0 ? (r.kmRecorridos / rendimiento) * precioGalon : 0;
        r.comision = r.gananciasDelDia * comisionPct;
        r.gananciaNeta = r.gananciasDelDia + r.totalPropinas - r.combustible - r.comision;
        r.promedio = r.carrerasRealizadas > 0 ? r.gananciasDelDia / r.carrerasRealizadas : 0;
        return r;
    }
}