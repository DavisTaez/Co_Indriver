package com.example.controlindriver;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface CarreraDao {
    @Insert long insertar(Carrera carrera);
    @Update void actualizar(Carrera carrera);
    @Delete void borrar(Carrera carrera);

    @Query("SELECT * FROM carreras ORDER BY fecha DESC")
    List<Carrera> getTodas();

    @Query("SELECT * FROM carreras WHERE vehiculoId = :vehiculoId ORDER BY fecha DESC")
    List<Carrera> getPorVehiculo(int vehiculoId);
}