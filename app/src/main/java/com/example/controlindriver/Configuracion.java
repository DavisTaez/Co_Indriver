package com.example.controlindriver;
import android.content.Context;
import android.content.SharedPreferences;

public class Configuracion {
    private static final String PREFS = "config_app";
    private SharedPreferences prefs;

    public Configuracion(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public float getComisionPorcentaje() {
        return prefs.getFloat("comision", 6.29f);
    }

    public void setComisionPorcentaje(float v) {
        prefs.edit().putFloat("comision", v).apply();
    }

    public float getPrecioGalon() {
        return prefs.getFloat("precio_galon", 3.245f);
    }

    public void setPrecioGalon(float v) {
        prefs.edit().putFloat("precio_galon", v).apply();
    }

    public float getRendimiento() {
        return prefs.getFloat("rendimiento", 65f);
    }

    public void setRendimiento(float v) {
        prefs.edit().putFloat("rendimiento", v).apply();
    }

    public int getVehiculoActualId() {
        return prefs.getInt("vehiculo_actual", 1);
    }

    public void setVehiculoActualId(int id) {
        prefs.edit().putInt("vehiculo_actual", id).apply();
    }
}