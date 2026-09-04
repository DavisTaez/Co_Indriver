package com.example.controlindriver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvGananciasDia, tvCarrerasRealizadas, tvKmTotales, tvOdoInicioActivo;
    private Button btnFinalizarCarrera, btnMicCentral;
    private RecyclerView rvCarreras;

    private CarreraAdapter adapter;
    private List<Carrera> listaCarreras;
    private SharedPreferences sharedPreferences;

    private final BroadcastReceiver receptorCarreras = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int num = intent.getIntExtra("numero_carrera", 1);
            long odoInicio = intent.getLongExtra("odo_inicio", 0);
            long odoFin = intent.getLongExtra("odo_fin", 0);
            double valor = intent.getDoubleExtra("valor", 2.00);

            // Agregar nueva carrera registrada desde la bola flotante
            listaCarreras.add(0, new Carrera(num, odoInicio, odoFin, valor, true));
            adapter.notifyDataSetChanged();
            actualizarResumenCabecera();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("ControlDriverPrefs", MODE_PRIVATE);

        // Vistas
        tvGananciasDia = findViewById(R.id.tvGananciasDia);
        tvCarrerasRealizadas = findViewById(R.id.tvCarrerasRealizadas);
        tvKmTotales = findViewById(R.id.tvKmTotales);
        tvOdoInicioActivo = findViewById(R.id.tvOdoInicioActivo);
        btnFinalizarCarrera = findViewById(R.id.btnFinalizarCarrera);
        btnMicCentral = findViewById(R.id.btnMicCentral);

        // Configuración de RecyclerView
        rvCarreras = findViewById(R.id.rvCarreras);
        rvCarreras.setLayoutManager(new LinearLayoutManager(this));

        listaCarreras = new ArrayList<>();
        adapter = new CarreraAdapter(listaCarreras);
        rvCarreras.setAdapter(adapter);

        // Registrar BroadcastReceiver para escuchar la bola flotante
        IntentFilter filter = new IntentFilter("ACCION_ACTUALIZAR_CARRERAS");

        ContextCompat.registerReceiver(
                this,
                receptorCarreras,
                filter,
                ContextCompat.RECEIVER_EXPORTED
        );

        btnMicCentral.setOnClickListener(v -> {
            Intent intent = new Intent(this, FloatingWidgetService.class);
            startService(intent);
            Toast.makeText(this, "Bola Flotante Lista", Toast.LENGTH_SHORT).show();
        });
    }

    private void actualizarResumenCabecera() {
        double totalGanancias = 0.0;
        long totalKm = 0;

        for (Carrera c : listaCarreras) {
            totalGanancias += c.getValor();
            totalKm += c.getKmRecorridos();
        }

        tvGananciasDia.setText(String.format(Locale.getDefault(), "$%.2f", totalGanancias));
        tvCarrerasRealizadas.setText(String.valueOf(listaCarreras.size()));
        tvKmTotales.setText(totalKm + " km");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receptorCarreras);
    }
}