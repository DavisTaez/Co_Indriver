package com.example.controlindriver;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;

public class ControlMantenimiento {

    private static final String PREFS = "mantenimiento_prefs";
    private static final String KEY_ULTIMO_ACEITE = "ultimo_aceite_km";
    private static final int INTERVALO_ACEITE = 5000;

    public static class Alerta {
        public String mensaje;
        public int kmRestantes;
        public boolean esUrgente;
    }

    public int getKmTotales(List<Carrera> lista) {
        int total = 0;
        for (Carrera c : lista) {
            if (!c.cancelada) total += c.getKmRecorridos();
        }
        return total;
    }

    public Alerta verificarAceite(Context context, List<Carrera> todas) {
        int kmTotales = getKmTotales(todas);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int ultimoCambio = prefs.getInt(KEY_ULTIMO_ACEITE, 0);

        int kmDesdeCambio = kmTotales - ultimoCambio;
        int kmRestantes = INTERVALO_ACEITE - kmDesdeCambio;

        Alerta alerta = new Alerta();
        alerta.kmRestantes = kmRestantes;
        alerta.esUrgente = kmRestantes <= 500;

        if (kmRestantes <= 0) {
            alerta.mensaje = "⚠ CAMBIO DE ACEITE ATRASADO por " + Math.abs(kmRestantes) + " km";
        } else {
            alerta.mensaje = "Aceite: te faltan " + kmRestantes + " km";
        }
        return alerta;
    }

    public void registrarCambioAceite(Context context, List<Carrera> todas) {
        int kmTotales = getKmTotales(todas);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_ULTIMO_ACEITE, kmTotales).apply();
    }
}
