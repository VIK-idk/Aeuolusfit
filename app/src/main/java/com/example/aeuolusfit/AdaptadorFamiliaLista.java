package com.example.aeuolusfit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdaptadorFamiliaLista extends RecyclerView.Adapter<AdaptadorFamiliaLista.ViewHolder> {

    private final List<Familiar> listaFamiliares;
    private OnItemClickListener listener;

    // Interfaz para manejar el clic en los elementos
    public interface OnItemClickListener {
        void onItemClick(Familiar familiar);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public AdaptadorFamiliaLista(List<Familiar> listaFamiliares) {
        this.listaFamiliares = listaFamiliares;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Asegúrate de que el layout sea el correcto de tu elemento de lista
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_familiar_tarjeta, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Familiar familiar = listaFamiliares.get(position);
        holder.tvNombre.setText(familiar.getNombre());
        holder.tvSensacion.setText(familiar.getSensacionTermica());

        // 🔴 Configurar el evento de clic en la tarjeta para que responda al toque
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(familiar);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaFamiliares.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvSensacion;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ajusta estos IDs según los TextViews reales de tu layout item_familiar_tarjeta.xml
            tvNombre = itemView.findViewById(R.id.tvNombreFamiliarItem);
            tvSensacion = itemView.findViewById(R.id.tvSensacionFamiliarItem);
        }
    }
}