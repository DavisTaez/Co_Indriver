package com.example.controlindriver;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

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
    private Button btnBotonVoz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas de la interfaz
        tvEstadoVoz = findViewById(R.id.tvEstadoVoz);
        tvTotalGanancias = findViewById(R.id.tvTotalGanancias);
        tvTotalKm = findViewById(R.id.tvTotalKm);
        btnBotonVoz = findViewById(R.id.btnBotonVoz);
        rvCarreras = findViewById(R.id.rvCarreras);

        // Configurar RecyclerView
        listaCarreras = new ArrayList<>();
        adaptadorCarreras = new AdaptadorCarreras(listaCarreras, this);
        rvCarreras.setLayoutManager(new LinearLayoutManager(this));
        rvCarreras.setAdapter(adaptadorCarreras);

        // Verificar y solicitar permisos de micrófono
        solicitarPermisoMicrofono();

        // Inicializar manejador de voz
        manejadorVoz = new ManejadorVozCarrera(this, this);

        // Acción del botón principal de voz
        btnBotonVoz.setOnClickListener(v -> {
            if (validarPermisos()) {
                manejadorVoz.presionarBotonCarrera();
            } else {
                solicitarPermisoMicrofono();
            }
        });
    }

    // --- CALLBACKS DE MANEJADOR DE VOZ ---

    @Override
    public void onCarreraIniciada(int numero, long odoInicio, double valor) {
        Carrera nuevaCarrera = new Carrera(numero, odoInicio, 0, valor, false);
        listaCarreras.add(nuevaCarrera);
        adaptadorCarreras.notifyItemInserted(listaCarreras.size() - 1);
        recalcularTotalesDelDia();
    }

    @Override
    public void onCarreraFinalizada(Carrera carreraFinalizada) {
        for (int i = 0; i < listaCarreras.size(); i++) {
            if (listaCarreras.get(i).getNumero() == carreraFinalizada.getNumero()) {
                listaCarreras.set(i, carreraFinalizada);
                adaptadorCarreras.notifyItemChanged(i);
                break;
            }
        }
        recalcularTotalesDelDia();
    }

    @Override
    public void onEstadoCambiado(ManejadorVozCarrera.EstadoVoz estadoActual, String mensaje) {
        runOnUiThread(() -> {
            if (tvEstadoVoz != null) {
                tvEstadoVoz.setText(mensaje);
            }
        });
    }

    // --- LÓGICA DE EDICIÓN MANUAL DESDE EL LÁPIZ ---

    @Override
    public void onEditarCarrera(Carrera carrera, int posicion) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_editar_carrera, null);

        EditText etOdoInicio = dialogView.findViewById(R.id.etOdoInicio);
        EditText etOdoFin = dialogView.findViewById(R.id.etOdoFin);
        EditText etValor = dialogView.findViewById(R.id.etValor);

        etOdoInicio.setText(String.valueOf(carrera.getOdoInicio()));
        etOdoFin.setText(carrera.getOdoFin() > 0 ? String.valueOf(carrera.getOdoFin()) : "");
        etValor.setText(carrera.getValor() > 0 ? String.valueOf(carrera.getValor()) : "");

        new AlertDialog.Builder(this)
                .setTitle("Editar Carrera")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String txtInicio = etOdoInicio.getText().toString().trim();
                    String txtFin = etOdoFin.getText().toString().trim();
                    String txtValor = etValor.getText().toString().trim();

                    if (!txtInicio.isEmpty()) {
                        long nuevoOdoInicio = Long.parseLong(txtInicio);
                        carrera.setOdoInicio(nuevoOdoInicio);
                        double nuevoValor = Double.parseDouble(txtValor);
                        carrera.setValor(nuevoValor);


                        // --- SOLUCIÓN AL PROBLEMA ---
                        // Si la carrera editada es la que está en curso (odoFin es 0 o aún no finaliza),
                        // actualizamos la variable temporal que usa el flujo de voz.
                        if (carrera.getOdoFin() == 0) {

                            // Opción B: Si odoInicioTemp está dentro de manejadorVozCarrera,
                            // actualizas la variable allí (descomenta si este es tu caso):
                            ManejadorVozCarrera.setOdoInicioTemp(nuevoOdoInicio);
                            ManejadorVozCarrera.setValorTemp(nuevoValor);
                        }
                    }

                    carrera.setOdoFin(txtFin.isEmpty() ? 0L : Long.parseLong(txtFin));
                    carrera.setValor(txtValor.isEmpty() ? 0.0 : Double.parseDouble(txtValor));

                    adaptadorCarreras.notifyItemChanged(posicion);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- CÁLCULOS Y PERMISOS ---

    private void recalcularTotalesDelDia() {
        double totalGanancias = 0.0;
        long totalKm = 0;

        for (Carrera c : listaCarreras) {
            totalGanancias += c.getValor();
            if (c.getOdoFin() >= c.getOdoInicio()) {
                totalKm += c.getKmRecorridos();
            }
        }

        if (tvTotalGanancias != null) {
            tvTotalGanancias.setText(String.format(Locale.US, "Total: $ %.2f", totalGanancias));
        }
        if (tvTotalKm != null) {
            tvTotalKm.setText("Km Recorridos: " + totalKm + " km");
        }
    }

    private boolean validarPermisos() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void solicitarPermisoMicrofono() {
        if (!validarPermisos()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, CODIGO_PERMISO_MICROFONO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODIGO_PERMISO_MICROFONO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de micrófono concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Se necesita permiso de micrófono para funcionar", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (manejadorVoz != null) {
            manejadorVoz.destruir();
        }
    }
}