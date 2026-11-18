package com.example.aeuolusfit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdaptadorChat extends RecyclerView.Adapter<AdaptadorChat.ViewHolder> {

    private final List<MensajeChat> lista;

    public AdaptadorChat(List<MensajeChat> lista) {
        this.lista = lista;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutIA, layoutUsuario;
        TextView tvTextoIA, tvHoraIA, tvTextoUsuario, tvHoraUsuario;

        public ViewHolder(@NonNull View v) {
            super(v);
            layoutIA = v.findViewById(R.id.layoutMensajeIA);
            layoutUsuario = v.findViewById(R.id.layoutMensajeUsuario);
            tvTextoIA = v.findViewById(R.id.tvTextoIA);
            tvHoraIA = v.findViewById(R.id.tvHoraIA);
            tvTextoUsuario = v.findViewById(R.id.tvTextoUsuario);
            tvHoraUsuario = v.findViewById(R.id.tvHoraUsuario);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mensaje_chat, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MensajeChat m = lista.get(position);
        if (m.isEsUsuario()) {
            holder.layoutUsuario.setVisibility(View.VISIBLE);
            holder.layoutIA.setVisibility(View.GONE);
            holder.tvTextoUsuario.setText(m.getTexto());
            holder.tvHoraUsuario.setText(m.getHora());
        } else {
            holder.layoutIA.setVisibility(View.VISIBLE);
            holder.layoutUsuario.setVisibility(View.GONE);
            holder.tvTextoIA.setText(m.getTexto());
            holder.tvHoraIA.setText(m.getHora());
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }
}