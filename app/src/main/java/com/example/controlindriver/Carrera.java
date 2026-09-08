package com.example.controlindriver;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "carreras")
public class Carrera {
    @PrimaryKey(autoGenerate = true) public int id;
    public int numero;
    public long odoInicio;
    public long odoFin;
    public double valor;
    public double propina;
    public int vehiculoId; // <-- ESTE CAMPO FALTABA
    public long fecha;
    public boolean cancelada;

    public Carrera() { this.fecha = System.currentTimeMillis(); this.vehiculoId = 1; }

    @Ignore
    public Carrera(int numero, long odoInicio, long odoFin, double valor, double propina, int vehiculoId, boolean cancelada) {
        this.numero = numero; this.odoInicio = odoInicio; this.odoFin = odoFin;
        this.valor = valor; this.propina = propina; this.vehiculoId = vehiculoId;
        this.cancelada = cancelada; this.fecha = System.currentTimeMillis();
    }

    @Ignore
    public Carrera(int numero, long odoInicio, long odoFin, double valor, boolean cancelada) {
        this(numero, odoInicio, odoFin, valor, 0, 1, cancelada);
    }

    public int getKmRecorridos() { return odoFin > odoInicio ? (int)(odoFin - odoInicio) : 0; }
    public int getNumero() { return numero; }
    public long getOdoInicio() { return odoInicio; }
    public long getOdoFin() { return odoFin; }
    public double getValor() { return valor; }
    public double getPropina() { return propina; }
    public void setOdoInicio(long v) { odoInicio = v; }
    public void setOdoFin(long v) { odoFin = v; }
    public void setValor(double v) { valor = v; }
    public void setPropina(double v) { propina = v; }
}