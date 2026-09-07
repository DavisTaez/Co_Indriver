package com.example.controlindriver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;
import java.util.Locale;

public class AdaptadorCarreras extends RecyclerView.Adapter<AdaptadorCarreras.CarreraViewHolder> {

    public interface OnCarreraEditListener {
        void onEditarCarrera(Carrera carrera, int posicion);
    }

    private final List<Carrera> listaCarreras;
    private final OnCarreraEditListener listener;

    public AdaptadorCarreras(List<Carrera> listaCarreras, OnCarreraEditListener listener) {
        this.listaCarreras = listaCarreras;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarreraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carrera, parent, false);
        return new CarreraViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarreraViewHolder holder, int position) {
        Carrera carrera = listaCarreras.get(position);

        holder.tvNumeroCarrera.setText("Carrera #" + carrera.getNumero());
        holder.tvOdoInicio.setText("Odo Inicio: " + carrera.getOdoInicio() + " km");

        if (carrera.getOdoFin() > 0) {
            holder.tvOdoFin.setText("Odo Fin: " + carrera.getOdoFin() + " km (" + carrera.getKmRecorridos() + " km)");
        } else {
            holder.tvOdoFin.setText("Odo Fin: En curso...");
        }

        holder.tvValorCarrera.setText(String.format(Locale.US, "Valor: $ %.2f", carrera.getValor()));

        holder.btnEditarCarrera.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditarCarrera(carrera, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaCarreras.size();
    }

    public static class CarreraViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumeroCarrera, tvOdoInicio, tvOdoFin, tvValorCarrera;
        ImageButton btnEditarCarrera;

        public CarreraViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumeroCarrera = itemView.findViewById(R.id.tvNumeroCarrera);
            tvOdoInicio = itemView.findViewById(R.id.tvOdoInicio);
            tvOdoFin = itemView.findViewById(R.id.tvOdoFin);
            tvValorCarrera = itemView.findViewById(R.id.tvValorCarrera);
            btnEditarCarrera = itemView.findViewById(R.id.btnEditarCarrera);
        }
    }
}