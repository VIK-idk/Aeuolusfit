package com.example.aeuolusfit;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AdaptadorFamiliaLista extends RecyclerView.Adapter<AdaptadorFamiliaLista.ViewHolder> {

    public interface OnFamiliarClickListener {
        void onFamiliarClick(Familiar familiar);
    }

    private final List<Familiar> lista;
    private OnFamiliarClickListener listener;

    public AdaptadorFamiliaLista(List<Familiar> lista) {
        this.lista = lista;
    }

    public void setOnFamiliarClickListener(OnFamiliarClickListener listener) {
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardAvatar;
        TextView tvInicial, tvNombre, tvEdad, tvSensacion;

        public ViewHolder(View v) {
            super(v);
            cardAvatar = v.findViewById(R.id.cardAvatarFamilia);
            tvInicial = v.findViewById(R.id.tvInicialFamiliarItem);
            tvNombre = v.findViewById(R.id.tvNombreFamiliarItem);
            tvEdad = v.findViewById(R.id.tvEdadFamiliarItem);
            tvSensacion = v.findViewById(R.id.tvSensacionFamiliarItem);
        }

        public void bind(Familiar f) {
            tvInicial.setText(f.getInicial());
            tvNombre.setText(f.getNombre());
            tvEdad.setText(f.getEdad() + " años aprox.");
            tvSensacion.setText(f.getSensacionTermica());
            try {
                cardAvatar.setCardBackgroundColor(Color.parseColor(f.getColorHex()));
            } catch (Exception e) {
                cardAvatar.setCardBackgroundColor(Color.parseColor("#E8CEBF"));
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_familiar_tarjeta, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Familiar f = lista.get(position);
        holder.bind(f);
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onFamiliarClick(f);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }
}