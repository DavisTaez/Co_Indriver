package com.example.controlindriver;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface VehiculoDao {
    @Insert
    long insertar(Vehiculo v);

    @Update
    void actualizar(Vehiculo v);

    @Query("SELECT * FROM vehiculos ORDER BY id ASC")
    List<Vehiculo> getTodos();

    @Query("SELECT * FROM vehiculos WHERE id = :id LIMIT 1")
    Vehiculo getPorId(int id);
}