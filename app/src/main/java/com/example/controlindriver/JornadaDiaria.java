package com.example.controlindriver;

import android.content.Context;
import java.util.List;

public class JornadaDiaria {
    public int totalCarreras;
    public int totalKm;
    public double totalGanancias;
    public double combustible;
    public double comision;
    public double gananciaNeta;
    public double promedioPorCarrera;
    public double totalPropinas;

    // MÉTODO CORRECTO - Ahora necesita Context
    public static JornadaDiaria desdeLista(Context ctx, List<Carrera> listaHoy) {
        StatsController stats = new StatsController();
        StatsController.Resumen r = stats.calcular(ctx, listaHoy);

        JornadaDiaria j = new JornadaDiaria();
        j.totalCarreras = r.carrerasRealizadas;
        j.totalKm = r.kmRecorridos;
        j.totalGanancias = r.gananciasDelDia;
        j.combustible = r.combustible; // antes era combustibleEstimado
        j.comision = r.comision;
        j.gananciaNeta = r.gananciaNeta;
        j.promedioPorCarrera = r.promedio; // antes era promedioPorCarrera
        j.totalPropinas = r.totalPropinas;
        return j;
    }
}