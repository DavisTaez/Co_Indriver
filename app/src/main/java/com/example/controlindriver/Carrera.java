package com.example.controlindriver;

public class Carrera {
    private int numero;
    private long odoInicio;
    private long odoFin;
    private double valor;
    private boolean completada;

    public Carrera(int numero, long odoInicio, long odoFin, double valor, boolean completada) {
        this.numero = numero;
        this.odoInicio = odoInicio;
        this.odoFin = odoFin;
        this.valor = valor;
        this.completada = completada;
    }

    public int getNumero() { return numero; }
    public long getOdoInicio() { return odoInicio; }
    public long getOdoFin() { return odoFin; }
    public double getValor() { return valor; }
    public boolean isCompletada() { return completada; }
    public long getKmRecorridos() { return odoFin - odoInicio; }
}