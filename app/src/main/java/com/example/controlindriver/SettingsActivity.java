package com.example.controlindriver;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Configuracion config = new Configuracion(this);

        EditText etComision = findViewById(R.id.etComision);
        EditText etPrecio = findViewById(R.id.etPrecioGalon);
        EditText etRend = findViewById(R.id.etRendimiento);
        Button btnGuardar = findViewById(R.id.btnGuardarConfig);

        etComision.setText(String.valueOf(config.getComisionPorcentaje()));
        etPrecio.setText(String.valueOf(config.getPrecioGalon()));
        etRend.setText(String.valueOf(config.getRendimiento()));

        btnGuardar.setOnClickListener(v -> {
            try {
                config.setComisionPorcentaje(Float.parseFloat(etComision.getText().toString()));
                config.setPrecioGalon(Float.parseFloat(etPrecio.getText().toString()));
                config.setRendimiento(Float.parseFloat(etRend.getText().toString()));
                Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) { Toast.makeText(this, "Datos inválidos", Toast.LENGTH_SHORT).show(); }
        });
    }
}