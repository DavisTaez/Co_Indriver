package com.example.controlindriver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ManejadorVozCarrera.CallbackCarreraVoz, AdaptadorCarreras.OnCarreraEditListener {

    private static final int CODIGO_PERMISO_MICROFONO = 100;
    private ManejadorVozCarrera manejadorVoz;
    private RecyclerView rvCarreras;
    private AdaptadorCarreras adaptadorCarreras;
    private List<Carrera> listaCarreras;
    private TextView tvEstadoVoz, tvTotalGanancias, tvTotalKm;
    private Button btnBotonVoz, btnConfig;
    private AppDatabase db;
    private StatsController statsController;
    private Configuracion config;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = AppDatabase.getInstance(this);
        config = new Configuracion(this);
        statsController = new StatsController();

        if (db.vehiculoDao().getTodos().isEmpty()) {
            long idVeh = db.vehiculoDao().insertar(new Vehiculo("Mi Auto Principal", "XXX-000"));
            config.setVehiculoActualId((int) idVeh);
        }

        tvEstadoVoz = findViewById(R.id.tvEstadoVoz);
        tvTotalGanancias = findViewById(R.id.tvTotalGanancias);
        tvTotalKm = findViewById(R.id.tvTotalKm);
        btnBotonVoz = findViewById(R.id.btnBotonVoz);
        btnConfig = findViewById(R.id.btnConfig);
        rvCarreras = findViewById(R.id.rvCarreras);

        listaCarreras = new ArrayList<>(db.carreraDao().getPorVehiculo(config.getVehiculoActualId()));
        adaptadorCarreras = new AdaptadorCarreras(listaCarreras, this);
        rvCarreras.setLayoutManager(new LinearLayoutManager(this));
        rvCarreras.setAdapter(adaptadorCarreras);

        manejadorVoz = new ManejadorVozCarrera(this, this);

        // FIX: Ahora buscamos el número más alto, no el tamaño de la lista
        int maxNumero = 0;
        for (Carrera c : listaCarreras) if (c.numero > maxNumero) maxNumero = c.numero;
        manejadorVoz.setNumeroCarreraActual(maxNumero + 1);

        btnBotonVoz.setOnClickListener(v -> { if (validarPermisos()) manejadorVoz.presionarBotonCarrera(); else solicitarPermisoMicrofono(); });
        btnConfig.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        recalcularTotalesDelDia();
    }

    @Override protected void onResume() { super.onResume(); recalcularTotalesDelDia(); }

    @Override public void onCarreraIniciada(int numero, long odoInicio, double valor) {
        Carrera nueva = new Carrera(numero, odoInicio, 0, valor, 0, config.getVehiculoActualId(), false);
        long nuevoId = db.carreraDao().insertar(nueva);
        nueva.id = (int) nuevoId; // FIX IMPORTANTE: Guardamos el id real

        listaCarreras.add(0, nueva);
        adaptadorCarreras.notifyItemInserted(0);
        rvCarreras.scrollToPosition(0);
        recalcularTotalesDelDia();
    }

    @Override public void onCarreraFinalizada(Carrera carreraFinalizada) {
        carreraFinalizada.vehiculoId = config.getVehiculoActualId();
        boolean encontrada = false;
        for (int i = 0; i < listaCarreras.size(); i++) {
            if (listaCarreras.get(i).numero == carreraFinalizada.numero) {
                Carrera existente = listaCarreras.get(i);
                existente.odoFin = carreraFinalizada.odoFin;
                existente.valor = carreraFinalizada.valor;
                existente.propina = carreraFinalizada.propina;
                existente.cancelada = carreraFinalizada.cancelada;
                db.carreraDao().actualizar(existente); // Ahora si actualiza porque tiene id correcto
                listaCarreras.set(i, existente);
                adaptadorCarreras.notifyItemChanged(i);
                encontrada = true;
                break;
            }
        }
        if (!encontrada) {
            long nuevoId = db.carreraDao().insertar(carreraFinalizada);
            carreraFinalizada.id = (int) nuevoId;
            listaCarreras.add(0, carreraFinalizada);
            adaptadorCarreras.notifyItemInserted(0);
        }
        recalcularTotalesDelDia();
    }

    @Override public void onEstadoCambiado(ManejadorVozCarrera.EstadoVoz estadoActual, String mensaje) {
        runOnUiThread(() -> tvEstadoVoz.setText(mensaje));
    }

    @Override public void onEditarCarrera(Carrera carrera, int posicion) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_editar_carrera, null);
        EditText etOdoInicio = dialogView.findViewById(R.id.etOdoInicio);
        EditText etOdoFin = dialogView.findViewById(R.id.etOdoFin);
        EditText etValor = dialogView.findViewById(R.id.etValor);
        EditText etPropina = dialogView.findViewById(R.id.etPropina);
        etOdoInicio.setText(String.valueOf(carrera.getOdoInicio()));
        etOdoFin.setText(carrera.getOdoFin() > 0 ? String.valueOf(carrera.getOdoFin()) : "");
        etValor.setText(String.valueOf(carrera.getValor()));
        etPropina.setText(String.valueOf(carrera.getPropina()));

        new AlertDialog.Builder(this).setTitle("Editar Carrera " + carrera.numero).setView(dialogView)
                .setPositiveButton("Guardar", (d, w) -> {
                    try {
                        carrera.setOdoInicio(Long.parseLong(etOdoInicio.getText().toString()));
                        carrera.setOdoFin(etOdoFin.getText().toString().isEmpty() ? 0 : Long.parseLong(etOdoFin.getText().toString()));
                        carrera.setValor(Double.parseDouble(etValor.getText().toString()));
                        carrera.setPropina(Double.parseDouble(etPropina.getText().toString()));
                        db.carreraDao().actualizar(carrera);
                        if (carrera.getOdoFin() == 0) manejadorVoz.actualizarDatosEnCurso(carrera.getOdoInicio(), carrera.getValor());
                        adaptadorCarreras.notifyItemChanged(posicion);
                        recalcularTotalesDelDia();
                    } catch (Exception e) { Toast.makeText(this, "Datos inválidos", Toast.LENGTH_SHORT).show(); }
                }).setNegativeButton("Cancelar", null).show();
    }

    @Override public void onBorrarCarrera(Carrera carrera, int posicion) {
        if (carrera.getOdoFin() > 0 && !carrera.cancelada) {
            Toast.makeText(this, "No puedes borrar carreras completadas", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("¿Borrar Carrera #" + carrera.numero + "?")
                .setPositiveButton("Borrar", (d, w) -> {
                    db.carreraDao().borrar(carrera);
                    listaCarreras.remove(posicion);
                    adaptadorCarreras.notifyItemRemoved(posicion);
                    recalcularTotalesDelDia();
                }).setNegativeButton("Cancelar", null).show();
    }

    private void recalcularTotalesDelDia() {
        StatsController.Resumen resumen = statsController.calcular(this, listaCarreras);
        tvTotalGanancias.setText(String.format(Locale.US, "Total Hoy: $ %.2f | Neta: $ %.2f", resumen.gananciasDelDia, resumen.gananciaNeta));
        tvTotalKm.setText(String.format(Locale.US, "%d km | %d carreras | Comb: $%.2f | Com: $%.2f", resumen.kmRecorridos, resumen.carrerasRealizadas, resumen.combustible, resumen.comision));
    }

    private boolean validarPermisos() { return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED; }
    private void solicitarPermisoMicrofono() { if (!validarPermisos()) ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, CODIGO_PERMISO_MICROFONO); }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); }
    @Override protected void onDestroy() { super.onDestroy(); if (manejadorVoz != null) manejadorVoz.destruir(); }
}