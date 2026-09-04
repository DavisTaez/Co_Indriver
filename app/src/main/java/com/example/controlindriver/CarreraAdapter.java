package com.example.controlindriver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class CarreraAdapter extends RecyclerView.Adapter<CarreraAdapter.ViewHolder> {

    private final List<Carrera> listaCarreras;

    public CarreraAdapter(List<Carrera> listaCarreras) {
        this.listaCarreras = listaCarreras;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carrera, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Carrera c = listaCarreras.get(position);
        holder.tvNumero.setText("Carrera #" + c.getNumero());
        holder.tvValor.setText(String.format(Locale.getDefault(), "$%.2f", c.getValor()));
        holder.tvOdometros.setText(String.format(Locale.getDefault(), "📍 %,d → %,d km", c.getOdoInicio(), c.getOdoFin()));
        holder.tvKm.setText("📐 " + c.getKmRecorridos() + " km");
    }

    @Override
    public int getItemCount() {
        return listaCarreras.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumero, tvValor, tvOdometros, tvKm;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumero = itemView.findViewById(R.id.tvNumeroCarrera);
            tvValor = itemView.findViewById(R.id.tvValorCarrera);
            tvOdometros = itemView.findViewById(R.id.tvOdometros);
            tvKm = itemView.findViewById(R.id.tvKmRecorridos);
        }
    }
}