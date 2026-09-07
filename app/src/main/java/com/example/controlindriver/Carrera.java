package com.example.controlindriver;

public class Carrera {

    private int numero;
    private long odoInicio;
    private long odoFin;
    private double valor;
    private boolean finalizada;

    public Carrera(int numero, long odoInicio, long odoFin, double valor, boolean finalizada) {
        this.numero = numero;
        this.odoInicio = odoInicio;
        this.odoFin = odoFin;
        this.valor = valor;
        this.finalizada = finalizada;
    }

    // --- GETTERS ---

    public int getNumero() {
        return numero;
    }

    public long getOdoInicio() {
        return odoInicio;
    }

    public long getOdoFin() {
        return odoFin;
    }

    public double getValor() {
        return valor;
    }

    public boolean isFinalizada() {
        return finalizada;
    }

    public long getKmRecorridos() {
        if (odoFin >= odoInicio && odoFin > 0) {
            return odoFin - odoInicio;
        }
        return 0;
    }

    // --- SETTERS (Necesarios para la edición manual) ---

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public void setOdoInicio(long odoInicio) {
        this.odoInicio = odoInicio;
    }

    public void setOdoFin(long odoFin) {
        this.odoFin = odoFin;
        if (this.odoFin >= this.odoInicio) {
            this.finalizada = true;
        }
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public void setFinalizada(boolean finalizada) {
        this.finalizada = finalizada;
    }
}