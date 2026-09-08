package com.example.controlindriver;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vehiculos")
public class Vehiculo {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String nombre;
    public String placa;
    public int kmUltimoAceite;

    public Vehiculo(String nombre, String placa) {
        this.nombre = nombre;
        this.placa = placa;
        this.kmUltimoAceite = 0;
    }
}